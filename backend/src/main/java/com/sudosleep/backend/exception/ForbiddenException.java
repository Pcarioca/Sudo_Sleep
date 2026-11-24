package com.sudosleep.backend.exception;

/** Maps to 403 Forbidden (e.g. attempting to access another user's resources). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
