package com.credit.analysis.exception;

public class CreditIncompatibleException extends RuntimeException {
    public CreditIncompatibleException(String message) {
        super(message);
    }
}
