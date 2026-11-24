package com.sudosleep.backend.exception;

/** Maps to 401 Unauthorized. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
