package com.bankingsystem.transfer.exception;

/** Missing or incorrect {@code X-FIB-AUTH} header. Maps to 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
