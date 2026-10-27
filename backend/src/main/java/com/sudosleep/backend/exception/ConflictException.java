package com.sudosleep.backend.exception;

/** The request clashes with existing data, e.g. a duplicate name (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
