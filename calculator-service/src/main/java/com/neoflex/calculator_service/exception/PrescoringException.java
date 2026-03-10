package com.neoflex.calculator_service.exception;

/**
 * Выбрасывается при провале прескоринга — когда данные заявки
 * некорректны или не соответствуют базовым требованиям.
 */
public class PrescoringException extends RuntimeException {

    public PrescoringException(String message) {
        super(message);
    }
}