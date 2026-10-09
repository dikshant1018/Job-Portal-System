package com.jobportal.dao;

/** Unchecked error carrying a message that is safe to show to the user. */
public class DaoException extends RuntimeException {
    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
