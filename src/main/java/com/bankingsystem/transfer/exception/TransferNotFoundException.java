package com.bankingsystem.transfer.exception;

/** A directly requested transfer resource does not exist. Maps to 404. */
public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException(String id) {
        super("Transfer not found: " + id);
    }
}
