package ru.nivshin.excelannotations.utils;

import lombok.experimental.UtilityClass;
import ru.nivshin.excelannotations.exceptions.SystemException;

import java.lang.reflect.Constructor;

@UtilityClass
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class BeanUtils {

    public <T> T createInstance(final Class<T> entityClass) {
        T entityInstance;
        try {
            Constructor constructor = entityClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            entityInstance = (T) constructor.newInstance();
            return entityInstance;
        } catch (Exception ex) {
            throw new SystemException("Cannot instantiate " + entityClass.getCanonicalName(), ex);
        }
    }

}
