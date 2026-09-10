package com.bankingsystem.transfer.exception;

/** A directly requested account resource does not exist. Maps to 404. */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String iban) {
        super("Account not found: " + iban);
    }
}
