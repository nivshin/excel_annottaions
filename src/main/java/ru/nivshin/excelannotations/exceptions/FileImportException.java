package ru.nivshin.excelannotations.exceptions;

public class FileImportException extends Exception {

    public FileImportException(final String message) {
        super(message);
    }

    public FileImportException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public FileImportException(final Throwable cause) {
        super(cause);
    }

    public FileImportException(final String message, final Throwable cause, final boolean enableSuppression,
                               final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
