package ru.nivshin.excelannotations.exceptions;

public class AmbiguousBindingException extends RuntimeException {

    public AmbiguousBindingException(final String message) {
        super(message);
    }

    public AmbiguousBindingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public AmbiguousBindingException(final Throwable cause) {
        super(cause);
    }

    public AmbiguousBindingException(final String message, final Throwable cause, final boolean enableSuppression,
                                     final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
