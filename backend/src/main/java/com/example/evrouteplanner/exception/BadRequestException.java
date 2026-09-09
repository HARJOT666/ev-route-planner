package com.example.evrouteplanner.exception;

/** Thrown for invalid input or business-rule violations (e.g. email already used). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
