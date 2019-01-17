package ru.nivshin.excelannotations.engine.structures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Worksheet projection annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface ExcelWorksheetAnnotation {
    int workSheet() default 0;
    boolean multiTables() default false;
    int scanTerminationEmptyLines() default 10;
}
