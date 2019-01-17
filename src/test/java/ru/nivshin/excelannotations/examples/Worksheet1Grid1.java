package ru.nivshin.excelannotations.examples;

import lombok.Getter;
import lombok.Setter;
import ru.nivshin.excelannotations.engine.structures.CellAnnotation;
import ru.nivshin.excelannotations.engine.structures.DataGrid;
import ru.nivshin.excelannotations.engine.structures.ExcelWorksheetAnnotation;
import ru.nivshin.excelannotations.engine.structures.GridAnnotation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ExcelWorksheetAnnotation(multiTables = true, workSheet = 1)
public class Worksheet1Grid1 implements DataGrid {

    @CellAnnotation(row = 0, column = 1)
    private String name;

    @GridAnnotation(columnNamesRowIndex = 1, startRowIndex = 1, startColumnIndex = 1,
            excludeRowWithContent = { "Total" },
            columnHeaderTypes = { String.class, Date.class },
            columnDataTypes = { String.class, Double.class })
    private List<Map<Object, Object>> grid = new ArrayList<>();
}
