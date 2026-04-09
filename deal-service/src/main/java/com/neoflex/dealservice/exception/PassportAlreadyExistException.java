package com.neoflex.dealservice.exception;

public class PassportAlreadyExistException extends RuntimeException {
    public PassportAlreadyExistException(String message) {
        super(message);
    }
}
