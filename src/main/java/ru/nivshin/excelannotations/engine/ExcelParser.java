package ru.nivshin.excelannotations.engine;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import ru.nivshin.excelannotations.exceptions.FileImportException;

import java.io.IOException;
import java.nio.file.Path;

public interface ExcelParser {

    /**
     * Fills in the target by its annotations
     * @param srcFile
     * @param target
     * @throws FileImportException
     * @return investigated grid offset area
     */
    <T> InvestigatedArea extract(Path srcFile, T target)
            throws FileImportException, IllegalAccessException, InvalidFormatException, IOException;

    /**
     *  Fills in the target by its annotations
     * @param srcFile
     * @param target
     * @param offsetArea
     * @throws FileImportException
     * @return new grid offset area
     * @throws FileImportException
     */
    <T> InvestigatedArea extract(Path srcFile, T target, InvestigatedArea offsetArea)
            throws FileImportException, IOException, InvalidFormatException, IllegalAccessException;

}
