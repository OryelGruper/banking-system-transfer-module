package com.bankingsystem.transfer.exception;

/** Maps to 422. */
public class DailyLimitExceededException extends RuntimeException {
    public DailyLimitExceededException(String message) {
        super(message);
    }
}
