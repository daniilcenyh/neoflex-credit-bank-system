package com.neoflex.dealservice.global;

import com.neoflex.deal.dto.ErrorResponse;
import com.neoflex.dealservice.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.OffsetDateTime;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private ErrorResponse buildErrorResponse(String message, int status, String errorType, OffsetDateTime timestamp) {
        return new ErrorResponse(
                timestamp,
                status,
                errorType,
                message
        );
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClientNotFoundException(ClientNotFoundException ex) {
        log.error("Client not found: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                "Client Not Found",
                OffsetDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(StatementNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStatementNotFoundException(StatementNotFoundException ex) {
        log.error("Statement not found: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                "Statement Not Found",
                OffsetDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmptyFinishRegistrationException.class)
    public ResponseEntity<ErrorResponse> handleEmptyFinishRegistrationException(EmptyFinishRegistrationException ex) {
        log.error("Empty finish registration: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                "Empty Finish Registration",
                OffsetDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmptyStatementIdException.class)
    public ResponseEntity<ErrorResponse> handleEmptyStatementIdException(EmptyStatementIdException ex) {
        log.error("Empty statement ID: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                "Empty Statement ID",
                OffsetDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OfferNotSelectedException.class)
    public ResponseEntity<ErrorResponse> handleOfferNotSelectedException(OfferNotSelectedException ex) {
        log.error("Offer not selected: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                "Offer Not Selected",
                OffsetDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PassportAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handlePassportAlreadyExistException(PassportAlreadyExistException ex) {
        log.error("Passport already exists: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                ex.getMessage(),
                HttpStatus.CONFLICT.value(),
                "Passport Already Exists",
                OffsetDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: ", ex);
        ErrorResponse errorResponse = buildErrorResponse(
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                OffsetDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}