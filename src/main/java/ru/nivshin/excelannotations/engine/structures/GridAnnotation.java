package ru.nivshin.excelannotations.engine.structures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Worksheet grid projection annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface GridAnnotation {
    Class<?>[] columnHeaderTypes() default { String.class };
    Class<?>[] columnDataTypes() default {};
    int columnNamesRowIndex() default 0;
    int startRowIndex() default 0;
    int startColumnIndex() default 0;
    int endRowIndex() default 10000;
    int endColumnIndex() default 256;
    String[] excludeRowWithContent() default {};
}
