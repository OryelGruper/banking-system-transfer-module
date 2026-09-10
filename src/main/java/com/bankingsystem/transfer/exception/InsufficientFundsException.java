package com.bankingsystem.transfer.exception;

/** Maps to 422, matching the spec's example error response exactly. */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
