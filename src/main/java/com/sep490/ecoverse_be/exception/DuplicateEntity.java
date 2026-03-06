package com.sep490.ecoverse_be.exception;

public class DuplicateEntity extends RuntimeException {
    public DuplicateEntity(String message) {
        super(message);
    }
}
