package ru.nivshin.excelannotations.engine;

import org.apache.poi.ss.usermodel.Cell;

public interface CellProcessor {
    Object getCellData(final Class<?> fieldType, final Cell cell);
}
