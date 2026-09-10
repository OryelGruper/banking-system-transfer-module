package com.bankingsystem.transfer.exception;

/** A directly requested standing order resource does not exist. Maps to 404. */
public class StandingOrderNotFoundException extends RuntimeException {
    public StandingOrderNotFoundException(String id) {
        super("Standing order not found: " + id);
    }
}
