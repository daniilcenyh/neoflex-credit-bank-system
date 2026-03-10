package com.neoflex.calculator_service.exception;

/**
 * Выбрасывается при отказе в выдаче кредита по правилам скоринга,
 * а также при нарушении бизнес-правил прескоринга.
 */
public class ScoringException extends RuntimeException {

    public ScoringException(String message) {
        super(message);
    }
}
