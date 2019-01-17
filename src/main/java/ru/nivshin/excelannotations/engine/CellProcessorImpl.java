package ru.nivshin.excelannotations.engine;

import com.github.drapostolos.typeparser.TypeParser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.RichTextString;

import java.util.Date;

@Slf4j
@AllArgsConstructor
@SuppressWarnings(
        {"PMD.CyclomaticComplexity", "PMD.StdCyclomaticComplexity",
                "PMD.ModifiedCyclomaticComplexity"})
public class CellProcessorImpl implements CellProcessor {

    private final TypeParser typeParser;

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    @Override
    public Object getCellData(final Class<?> fieldType, final Cell cell) {

        if (cell == null) {
            return null;
        }

        Object cellValue = null;
        switch (cell.getCellTypeEnum()) {
            case BLANK:
                // empty;
                break;
            case BOOLEAN:
                cellValue = cell.getBooleanCellValue();
                break;
            case STRING:
                cellValue = extractString(cell);
                break;
            case NUMERIC:
                if (fieldType != null) {
                    if (fieldType.isAssignableFrom(Date.class)) {
                        cellValue = cell.getDateCellValue();
                    } else {
                        cellValue = cell.getNumericCellValue();
                    }
                }
                break;
            case FORMULA:
                if (fieldType == Boolean.class) {
                    cellValue = cell.getBooleanCellValue();
                } else if (fieldType == String.class) {
                    cellValue = extractString(cell);
                } else if (Number.class.isAssignableFrom(fieldType)) {
                    cellValue = cell.getNumericCellValue();
                } else if (Date.class.isAssignableFrom(fieldType)) {
                    cellValue = cell.getDateCellValue();
                } else {
                    log.warn("Cannot find appropriate formula type");
                }
                break;
            default:
                // empty
                break;
        }

        if (cellValue != null && fieldType != null && !fieldType.isAssignableFrom(cellValue.getClass())) {
            String stringCellValue = String.valueOf(cellValue).trim();
            try {
                cellValue = typeParser.parse(stringCellValue, fieldType);
            } catch (RuntimeException ex) {
                log.error("Cannot parse cell value for type " + fieldType.getCanonicalName()
                        + ". Value: " + stringCellValue, ex);
            }
        }

        return cellValue;
    }

    private String extractString(final Cell cell) {
        RichTextString string = cell.getRichStringCellValue();
        return string.toString().trim();
    }
}
