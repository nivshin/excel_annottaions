package ru.nivshin.excelannotations.engine.structures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cell projection annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface CellAnnotation {
    int row() default -1;
    int column() default -1;
}
