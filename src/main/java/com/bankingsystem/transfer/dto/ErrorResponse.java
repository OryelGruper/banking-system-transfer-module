package com.bankingsystem.transfer.dto;

import java.time.LocalDateTime;

/** Matches the error response shape written in task specification. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
