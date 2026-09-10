package com.bankingsystem.transfer.exception;

/**
 * A transfer precondition failed: missing account, non-positive amount,
 * or source == destination. Maps to 422.
 */
public class InvalidTransferException extends RuntimeException {
    public InvalidTransferException(String message) {
        super(message);
    }
}
