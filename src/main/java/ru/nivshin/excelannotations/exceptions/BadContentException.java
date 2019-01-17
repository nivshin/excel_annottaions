package ru.nivshin.excelannotations.exceptions;

public class BadContentException extends RuntimeException {

    public BadContentException(final String message) {
        super(message);
    }

    public BadContentException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public BadContentException(final Throwable cause) {
        super(cause);
    }

    public BadContentException(final String message, final Throwable cause, final boolean enableSuppression,
                               final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
