package ru.nivshin.excelannotations.engine;


import com.github.drapostolos.typeparser.TypeParser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import ru.nivshin.excelannotations.engine.structures.DataGrid;
import ru.nivshin.excelannotations.examples.UnnecessaryWorksheet1Grid2;
import ru.nivshin.excelannotations.examples.Worksheet0Grid;
import ru.nivshin.excelannotations.examples.Worksheet1Grid1;
import ru.nivshin.excelannotations.examples.Worksheet1Grid3;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public class ExcelParserTest {

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private enum TestEnum {

        XLS_EXCEL_FILE1("example1.xls", ExcelFileFormat.EXCEL_XLS_97_2007),
        XLS_EXCEL_FILE2("example2.xls", ExcelFileFormat.EXCEL_XLS_97_2007),
        XLSX_EXCEL_FILE1("example1.xlsx", ExcelFileFormat.EXCEL_XLSX_OOXML_2007),
        XLSX_EXCEL_FILE2("example2.xlsx", ExcelFileFormat.EXCEL_XLSX_OOXML_2007);

        private String fileName;

        private ExcelFileFormat format;
    }

    private ExcelParser excelParser = new ExcelParserImpl(new CellProcessorImpl(TypeParser.newBuilder().build()));

    /**
     * Testing composite table worksheet mode
     * @throws Exception
     */
    @Test
    public void testReadWorksheetTransitively() throws Exception {

        for (TestEnum test : TestEnum.values()) {

            ClassLoader classLoader = getClass().getClassLoader();
            Path file = new File(classLoader.getResource(test.fileName).getFile()).toPath();
            Worksheet0Grid worksheetObject = new Worksheet0Grid();
            InvestigatedArea area = excelParser.extract(file, worksheetObject);

            assertThat(area.getUpperLeftX()).isEqualTo(1);
            assertThat(area.getBottomRightX()).isEqualTo(44);
            assertThat(area.getUpperLeftY()).isEqualTo(0);
            assertThat(area.getBottomRightY()).isEqualTo(30);

            assertThat(worksheetObject.getName()).isNotBlank();
            assertThat(worksheetObject.getName()).startsWith("Header 1");
            assertThat(worksheetObject.getGrid()).hasSize(26);

            checkGrid(worksheetObject, new Class<?>[]{String.class, Date.class},
                    new Class<?>[]{String.class, Double.class});
        }
    }

    /**
     * Testing multi-table worksheet mode
     * @throws Exception
     */
    @Test
    public void testReadWorksheetAsMultiTable() throws Exception {

        for (TestEnum test : TestEnum.values()) {

            ClassLoader classLoader = getClass().getClassLoader();
            Path file = new File(classLoader.getResource(test.fileName).getFile()).toPath();

            Worksheet1Grid1 firstWorksheetObject = new Worksheet1Grid1();
            InvestigatedArea area = excelParser.extract(file, firstWorksheetObject);
            assertThat(area.getUpperLeftX()).isEqualTo(1);
            assertThat(area.getBottomRightX()).isEqualTo(44);
            assertThat(area.getUpperLeftY()).isEqualTo(0);
            assertThat(area.getBottomRightY()).isEqualTo(19);

            checkGrid(firstWorksheetObject,
                    new Class<?>[]{String.class, Date.class}, new Class<?>[]{String.class, Double.class});

            UnnecessaryWorksheet1Grid2 toSkip = new UnnecessaryWorksheet1Grid2();
            area = excelParser.extract(file, toSkip, area);
            assertThat(area.getUpperLeftX()).isEqualTo(1);
            assertThat(area.getBottomRightX()).isEqualTo(44);
            assertThat(area.getUpperLeftY()).isEqualTo(0);
            assertThat(area.getBottomRightY()).isEqualTo(32);

            Worksheet1Grid3 secondWorksheetObject = new Worksheet1Grid3();
            area = excelParser.extract(file, secondWorksheetObject, area);
            assertThat(area.getUpperLeftX()).isEqualTo(1);
            assertThat(area.getBottomRightX()).isEqualTo(44);
            assertThat(area.getUpperLeftY()).isEqualTo(0);
            assertThat(area.getBottomRightY()).isEqualTo(39);

            checkGrid(secondWorksheetObject,
                    new Class<?>[]{String.class, Date.class}, new Class<?>[]{String.class, Double.class});
        }
    }

    private void checkGrid(DataGrid dataGrid, Class<?>[] columnHeaderTypes, Class<?>[] columnDataTypes) {
        for (Map<Object, Object> record : dataGrid.getGrid()) {
            // iterating over rows
            for (Map.Entry<Object, Object> en : record.entrySet()) {
                // iterating over row cells
                assertThat(en.getKey()).isNotNull();
                assertThat(Arrays.stream(columnHeaderTypes).filter(e -> e == en.getKey().getClass())
                        .findAny().isPresent()).isTrue();
                if (en.getValue() != null) {
                    assertThat(Arrays.stream(columnDataTypes).filter(e -> e == en.getValue().getClass())
                            .findAny().isPresent()).isTrue();
                }
            }
        }
    }
}
