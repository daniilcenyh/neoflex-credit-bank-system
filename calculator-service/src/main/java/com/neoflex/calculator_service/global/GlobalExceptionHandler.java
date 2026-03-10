package com.neoflex.calculator_service.global;

import com.neoflex.calculator_service.exception.PrescoringException;
import com.neoflex.calculator_service.exception.ScoringException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.calculator.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.View;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final View view;

    private ErrorResponse buildErrorResponse(String message, int status, String errorType, OffsetDateTime timestamp) {
        return new ErrorResponse(
                timestamp,
                status,
                errorType,
                message
        );
    }

    @ExceptionHandler(PrescoringException.class)
    public ResponseEntity<ErrorResponse> handlePrescoringException(PrescoringException e) {
        log.error("Ошибка прескоринга: {}", e.getMessage());
        var timestamp = OffsetDateTime.now();
        int status = HttpStatus.BAD_REQUEST.value();
        String errorType = "Ошибка прескоринга";
        String message = e.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(message, status, errorType, timestamp));
    }

    @ExceptionHandler(ScoringException.class)
    public ResponseEntity<ErrorResponse> handleScoringException(ScoringException e) {
        log.error("Ошибка скоринга: {}", e.getMessage());
        log.error("Ошибка скоринга: {}", e.getMessage());
        var timestamp = OffsetDateTime.now();
        int status = HttpStatus.BAD_REQUEST.value();
        String errorType = "Ошибка скоринга";
        String message = e.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(message, status, errorType, timestamp));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Ошибка валидации: {}", errors);
        var timestamp = OffsetDateTime.now();
        int status = HttpStatus.BAD_REQUEST.value();
        String errorType = "Ошибка валидации";
        String message = errors.toString();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(message, status, errorType, timestamp));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Непредвиденная ошибка: ", e);
        var timestamp = OffsetDateTime.now();
        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String errorType = "Внутренняя ошибка сервера";
        String message = e.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(message, status, errorType, timestamp));
    }


}
