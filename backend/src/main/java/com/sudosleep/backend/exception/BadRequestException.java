package com.sudosleep.backend.exception;

/** The request is well-formed but breaks a business rule (HTTP 400). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
