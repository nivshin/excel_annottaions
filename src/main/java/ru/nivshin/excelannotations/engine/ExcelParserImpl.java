package ru.nivshin.excelannotations.engine;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import ru.nivshin.excelannotations.engine.structures.CellAnnotation;
import ru.nivshin.excelannotations.engine.structures.ExcelWorksheetAnnotation;
import ru.nivshin.excelannotations.engine.structures.GridAnnotation;
import ru.nivshin.excelannotations.exceptions.AmbiguousBindingException;
import ru.nivshin.excelannotations.exceptions.BadContentException;
import ru.nivshin.excelannotations.exceptions.FileImportException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@SuppressWarnings(
        {"PMD.GodClass", "PMD.CyclomaticComplexity", "PMD.StdCyclomaticComplexity",
                "PMD.ModifiedCyclomaticComplexity", "PMD.NullAssignment", "PMD.AccessorMethodGeneration"})
@AllArgsConstructor
public class ExcelParserImpl implements ExcelParser {

    private final CellProcessor cellProcessor;

    private class Limits {

        private int startDataColIndex;

        private int endDataColIndex;

        private int currentDataRowIndex;
    }

    private void processCell(
            final Field field, final Object object,
            final CellAnnotation cellAnnotation, final Sheet sheet,
            final int firstRowIdx, final int lastRowIdx,
            final InvestigatedArea area) throws IllegalAccessException {

        if (cellAnnotation.row() < firstRowIdx || cellAnnotation.row() > lastRowIdx) {
            throw new IllegalArgumentException("The cell data are out of worksheet data range");
        }
        area.extendArea(cellAnnotation.column(), cellAnnotation.row());

        Row row = sheet.getRow(cellAnnotation.row());
        Cell cell = row.getCell(cellAnnotation.column());
        Object cellValue = cellProcessor.getCellData(field.getType(), cell);
        field.setAccessible(true);
        field.set(object, cellValue);
    }

    @SuppressWarnings({"checkstyle:parameternumber", "PMD.NPathComplexity"})
    private void processGrid(
            final Field field, final Object object,
            final ExcelWorksheetAnnotation entityAnnotation, final GridAnnotation gridAnnotation,
            final Sheet sheet, final int firstRowIndex, final int lastRowIndex,
            final InvestigatedArea offsetArea) throws IllegalAccessException {

        int headerRowIndex = gridAnnotation.columnNamesRowIndex();
        boolean isColumnHeadersAbsent = headerRowIndex < 0;
        if (!isColumnHeadersAbsent && (headerRowIndex < firstRowIndex || headerRowIndex > lastRowIndex)) {
            throw new IllegalArgumentException("The cell data are out of worksheet data range");
        }

        // Correct limits by file content
        int startRowIndex =
                gridAnnotation.startRowIndex() < firstRowIndex ? firstRowIndex : gridAnnotation.startRowIndex();
        startRowIndex = startRowIndex > lastRowIndex ? lastRowIndex : startRowIndex;
        int endRowIndex = gridAnnotation.endRowIndex() < firstRowIndex ? firstRowIndex : gridAnnotation.endRowIndex();
        endRowIndex = endRowIndex > lastRowIndex ? lastRowIndex : endRowIndex;

        Map<Integer, Object> columnHeaders;
        if (entityAnnotation.multiTables()) {
            columnHeaders = new HashMap<>();
        } else {
            columnHeaders = offsetArea.getColumnHeaders();
        }

        Limits limits;
        // Headers processing
        if (gridAnnotation.columnNamesRowIndex() >= startRowIndex
                && endRowIndex >= gridAnnotation.columnNamesRowIndex()) {
            limits = fillInColumnHeaders(columnHeaders, gridAnnotation, sheet, offsetArea);
        } else {
            limits = fillInColumnHeadersImplicitly(columnHeaders, entityAnnotation, gridAnnotation, sheet, offsetArea);
        }
        // Collecting data from grid rows
        collectGridData(field, object, entityAnnotation, gridAnnotation, columnHeaders, limits, sheet, endRowIndex,
                offsetArea);
    }

    /**
     * Column headers row is defined implicitly, it can be missing
     *
     * @param columnHeaders
     * @param entityAnnotation
     * @param gridAnnotation
     * @param sheet
     * @param offsetArea
     * @return limits
     */
    @SuppressWarnings({"PMD.ExcessiveMethodLength", "PMD.NPathComplexity"})
    private Limits fillInColumnHeadersImplicitly(
            final Map<Integer, Object> columnHeaders, final ExcelWorksheetAnnotation entityAnnotation,
            final GridAnnotation gridAnnotation, final Sheet sheet, final InvestigatedArea offsetArea) {

        Limits result = new Limits();

        int emptyLinesLimit = entityAnnotation.scanTerminationEmptyLines();
        int scanRowIndex;
        int scanStartColIndex;

        if (entityAnnotation.multiTables()) {
            if (offsetArea.isEmpty()) {
                scanRowIndex = gridAnnotation.startRowIndex();
            } else {
                scanRowIndex = offsetArea.getBottomRightY() + 1;
            }
            scanStartColIndex = gridAnnotation.startColumnIndex();
        } else {
            if (offsetArea.isEmpty()) {
                scanRowIndex = gridAnnotation.startRowIndex();
                scanStartColIndex = gridAnnotation.startColumnIndex();
            } else {
                scanRowIndex = offsetArea.getBottomRightY() + 1;
                scanStartColIndex = offsetArea.getUpperLeftX();
            }
        }
        while (true) {
            if (sheet.getRow(scanRowIndex) != null) {
                Row row = sheet.getRow(scanRowIndex);
                Map<Integer, Class<?>> columnTypes;
                if (entityAnnotation.multiTables()) {
                    // Each table has its own column headers
                    columnTypes = getColumnTypes(gridAnnotation.startColumnIndex(),
                            findLastColumnIndex(gridAnnotation.startColumnIndex(), row),
                            gridAnnotation.columnHeaderTypes());
                } else {
                    // Continues filling in the previous table, but no previous table headers exist
                    columnTypes = getColumnTypes(gridAnnotation.startColumnIndex(),
                            findLastColumnIndex(gridAnnotation.startColumnIndex(), row),
                            gridAnnotation.columnDataTypes());
                }
                StopReason stopReason = needToSkipRow(row, gridAnnotation, columnTypes, scanRowIndex,
                        scanStartColIndex, offsetArea);
                if (stopReason != StopReason.DONT_STOP) {
                    offsetArea.extendArea(scanStartColIndex
                            + (columnTypes.isEmpty() ? 0 : columnTypes.size() - 1), scanRowIndex);
                    scanRowIndex++;
                    if (stopReason == StopReason.STOP_REASON_EMPTY_ROW) {
                        emptyLinesLimit--;
                    }
                    if (emptyLinesLimit < 0) {
                        throw new BadContentException("Too many empty lines before the content");
                    }
                    continue;
                }

                boolean noHeaders = false;
                for (int i = scanStartColIndex; i <= scanStartColIndex + columnTypes.size() - 1; i++) {
                    Cell cell = row.getCell(i);
                    offsetArea.extendArea(i, scanRowIndex);
                    if (!isEmptyCell(cell)) {
                        Class<?> columnType = columnTypes.get(i);
                        Object cellValue = cellProcessor.getCellData(columnType, cell);
                        if (cellValue != null) {
                            if (columnHeaders.isEmpty()) {
                                result.startDataColIndex = i;
                            }
                            result.endDataColIndex = i;
                            if (entityAnnotation.multiTables()) {
                                // Each table has its own column headers
                                columnHeaders.put(i, cellValue);
                                result.currentDataRowIndex = scanRowIndex + 1;
                            } else {
                                if (columnHeaders.isEmpty()) {
                                    // Continues filling in the previous table, but no previous table headers exist
                                    noHeaders = true;
                                }
                                if (noHeaders) {
                                    columnHeaders.put(i, String.valueOf(i));
                                }
                                result.currentDataRowIndex = scanRowIndex;
                            }
                        }
                    } else {
                        result.endDataColIndex = i - 1;
                        break;
                    }
                }
                if (!columnHeaders.isEmpty()) {
                    break;
                }
                scanRowIndex++;
                emptyLinesLimit--;
                if (emptyLinesLimit < 0) {
                    throw new BadContentException("Too many empty lines before the content");
                }
            } else {
                scanRowIndex++;
            }
        }

        return result;
    }


    /**
     * Column headers row is defined explicitly
     *
     * @param columnHeaders
     * @param gridAnnotation
     * @param sheet
     * @param offsetArea
     * @return
     */
    private Limits fillInColumnHeaders(
            final Map<Integer, Object> columnHeaders, final GridAnnotation gridAnnotation,
            final Sheet sheet, final InvestigatedArea offsetArea) {

        Limits result = new Limits();
        Row row = sheet.getRow(gridAnnotation.columnNamesRowIndex());
        Map<Integer, Class<?>> columnTypes = getColumnTypes(gridAnnotation.startColumnIndex(),
                findLastColumnIndex(gridAnnotation.startColumnIndex(), row),
                gridAnnotation.columnHeaderTypes());

        result.currentDataRowIndex = gridAnnotation.columnNamesRowIndex() + 1;

        for (int i = gridAnnotation.startColumnIndex(); gridAnnotation.endColumnIndex() < 0
                || i <= gridAnnotation.endColumnIndex(); i++) {

            Cell cell = row.getCell(i);
            offsetArea.extendArea(i, gridAnnotation.columnNamesRowIndex());
            if (!isEmptyCell(cell)) {
                Class<?> columnType = columnTypes.get(i);
                Object cellValue = cellProcessor.getCellData(columnType, cell);
                if (cellValue != null) {
                    if (columnHeaders.isEmpty()) {
                        result.startDataColIndex = i;
                    }
                    columnHeaders.put(i, cellValue);
                    result.endDataColIndex = i;
                }
            } else {
                result.endDataColIndex = i - 1;
                break;
            }
        }
        return result;
    }

    @SuppressWarnings({"checkstyle:parameternumber", "PMD.NPathComplexity", "PMD.PrematureDeclaration"})
    private void collectGridData(
            final Field field, final Object object, final ExcelWorksheetAnnotation entityAnnotation,
            final GridAnnotation gridAnnotation, final Map<Integer, Object> columnHeaders, final Limits limits,
            final Sheet sheet, final int endRowIndex, final InvestigatedArea offsetArea)
            throws IllegalAccessException {

        int currentExcelRowIndex = limits.currentDataRowIndex;
        int emptyLinesLimit = entityAnnotation.scanTerminationEmptyLines();
        Map<Integer, Class<?>> columnTypes = getColumnTypes(
                limits.startDataColIndex, limits.endDataColIndex, gridAnnotation.columnDataTypes());
        field.setAccessible(true);
        List<Map<Object, Object>> rows;
        if (List.class.isAssignableFrom(field.getType())) {
            rows = (List<Map<Object, Object>>) field.get(object);
            if (rows == null) {
                rows = new ArrayList<>();
            }
        } else {
            throw new BadContentException("The field annotated as @GridAnnotation should be a list");
        }
        int delta = -1;
        while (true) {
            if (endRowIndex < currentExcelRowIndex || emptyLinesLimit < 0) {
                break;
            }
            if (sheet.getRow(currentExcelRowIndex) != null) {
                Row row = sheet.getRow(currentExcelRowIndex);
                StopReason stopReason = needToSkipRow(row, gridAnnotation, columnTypes, currentExcelRowIndex,
                        limits.startDataColIndex,
                        offsetArea);
                if (stopReason != StopReason.DONT_STOP) {
                    offsetArea.extendArea(limits.endDataColIndex, currentExcelRowIndex);
                    currentExcelRowIndex++;
                    delta++;
                    if (stopReason == StopReason.STOP_REASON_EMPTY_ROW) {
                        emptyLinesLimit--;
                    }
                    if (emptyLinesLimit < 0) {
                        break;
                    }
                    if (stopReason == StopReason.STOP_REASON_EMPTY_ROW
                            && !rows.isEmpty()
                            && entityAnnotation.multiTables()) {
                        break;
                    }
                    continue;
                }
                if (rows.isEmpty()) {
                    delta = currentExcelRowIndex;
                }
                if (currentExcelRowIndex - rows.size() - delta == 0) {
                    rows.add(new HashMap<>());
                }

                Map<Object, Object> mappedRow = rows.get(currentExcelRowIndex - delta);
                for (int i = limits.startDataColIndex; i <= limits.endDataColIndex; i++) {
                    offsetArea.extendArea(i, currentExcelRowIndex);
                    Cell cell = row.getCell(i);
                    Class<?> columnType = columnTypes.get(i);
                    Object cellValue = cellProcessor.getCellData(columnType, cell);
                    Object columnHeader = columnHeaders.get(i);
                    mappedRow.put(columnHeader, cellValue);
                }
                currentExcelRowIndex++;
            } else {
                if (entityAnnotation.multiTables()) {
                    emptyLinesLimit--;
                    if (emptyLinesLimit < 0 || !rows.isEmpty()) {
                        break;
                    }
                } else {
                    currentExcelRowIndex++;
                    delta++;
                }
            }
        }
        field.set(object, rows);
    }

    private int findLastColumnIndex(final int startDataColIndex, final Row row) {
        int colIndex = startDataColIndex;
        while (true) {
            Cell cell = row.getCell(colIndex);
            if (cell == null || cell.getCellTypeEnum() == CellType.BLANK) {
                colIndex--;
                break;
            }
            colIndex++;
        }
        return colIndex;
    }

    private Map<Integer, Class<?>> getColumnTypes(final int startDataColIndex, final int endDataColIndex,
                                                  final Class<?>... configuredHeaderTypes) {
        Class<?> currentColumnType = String.class;
        Map<Integer, Class<?>> columnTypes = new HashMap<>();
        for (int i = startDataColIndex; i <= endDataColIndex; i++) {
            if (configuredHeaderTypes.length > i - startDataColIndex) {
                currentColumnType = configuredHeaderTypes[i - startDataColIndex];
            }
            columnTypes.put(i, currentColumnType);
        }
        return columnTypes;
    }

    private boolean isEmptyCell(final Cell cell) {
        return cell == null || cell.getCellTypeEnum() == CellType.BLANK;
    }

    private StopReason needToSkipRow(
            final Row row, final GridAnnotation gridAnnotation, final Map<Integer, Class<?>> columnTypes,
            final int scanRowIndex, final int startDataColIndex, final InvestigatedArea offsetArea) {

        StopReason stopReason = null;
        boolean isEmptyRow = true;
        for (int i = startDataColIndex; i <= startDataColIndex + columnTypes.size() - 1; i++) {
            Cell cell = row.getCell(i);
            offsetArea.extendArea(i, scanRowIndex);
            if (!isEmptyCell(cell)) {
                isEmptyRow = false;
                Class<?> columnType = columnTypes.get(i);
                Object cellContent = cellProcessor.getCellData(columnType, cell);
                if (cellContent != null && Arrays.stream(gridAnnotation.excludeRowWithContent())
                        .anyMatch(e -> String.valueOf(cellContent).contains(e))) {
                    stopReason = StopReason.STOP_REASON_EXCLUDED_WORDS;
                }
            }
        }
        if (stopReason == null && isEmptyRow) {
            stopReason = StopReason.STOP_REASON_EMPTY_ROW;
        }
        if (stopReason == null) {
            stopReason = StopReason.DONT_STOP;
        }
        return stopReason;
    }

    private <T> void loadWorksheet(final Workbook wb, final ExcelWorksheetAnnotation entityAnnotation,
                                   final T target, final InvestigatedArea offsetArea) throws IllegalAccessException {
        Sheet sheet = wb.getSheetAt(entityAnnotation.workSheet());
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        for (Field field : target.getClass().getDeclaredFields()) {

            CellAnnotation cellAnnotation = field.getAnnotation(CellAnnotation.class);
            GridAnnotation gridAnnotation = field.getAnnotation(GridAnnotation.class);

            if (cellAnnotation == null && gridAnnotation == null) {
                continue;
            }
            if (cellAnnotation != null && gridAnnotation != null) {
                throw new AmbiguousBindingException("A field "
                        + field.getName()
                        + " can be bound either to a grid or single cell");
            }
            if (cellAnnotation != null) {
                processCell(field, target, cellAnnotation, sheet, firstRow, lastRow, offsetArea);
            } else {
                processGrid(field, target, entityAnnotation, gridAnnotation, sheet, firstRow, lastRow, offsetArea);
            }
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    @Override
    public <T> InvestigatedArea extract(
            final Path filePath, final T target, final InvestigatedArea offsetArea, final ExcelFileFormat format)
            throws FileImportException, IOException, InvalidFormatException, IllegalAccessException {

        if (!Files.exists(filePath)) {
            throw new FileImportException("The file " + filePath + " doesn't exist");
        }

        Workbook wb;
        try (InputStream is = new FileInputStream(filePath.toFile())) {
            wb = WorkbookFactory.create(is);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            throw ex;
        }
        ExcelWorksheetAnnotation entityAnnotation =
                target.getClass().getAnnotation(ExcelWorksheetAnnotation.class);
        loadWorksheet(wb, entityAnnotation, target, offsetArea);
        return offsetArea;
    }

    @Override
    public <T> InvestigatedArea extract(final Path filePath, final T target, final ExcelFileFormat format)
            throws FileImportException, IllegalAccessException, InvalidFormatException, IOException {
        InvestigatedArea area = new InvestigatedArea();
        return extract(filePath, target, area, format);
    }
}
