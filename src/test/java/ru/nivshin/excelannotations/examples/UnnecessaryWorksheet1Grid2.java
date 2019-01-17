package ru.nivshin.excelannotations.examples;

import lombok.Getter;
import lombok.Setter;
import ru.nivshin.excelannotations.engine.structures.DataGrid;
import ru.nivshin.excelannotations.engine.structures.ExcelWorksheetAnnotation;
import ru.nivshin.excelannotations.engine.structures.GridAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ExcelWorksheetAnnotation(multiTables = true, workSheet = 1)
public class UnnecessaryWorksheet1Grid2 implements DataGrid {

    @GridAnnotation(columnNamesRowIndex = -1, startColumnIndex = 1, endColumnIndex = 1,
            columnHeaderTypes = String.class,
            columnDataTypes = String.class)
    private List<Map<Object, Object>> grid = new ArrayList<>();
}
