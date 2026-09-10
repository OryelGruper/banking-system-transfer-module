package com.bankingsystem.transfer.dto;

import com.bankingsystem.transfer.domain.StandingOrder;

import java.math.BigDecimal;
import java.time.Instant;

public record StandingOrderResponse(
        String id,
        String sourceIban,
        String destinationIban,
        BigDecimal amount,
        String cronExpression,
        boolean active,
        Instant createdAt,
        Instant lastExecutedAt,
        String lastFailureMessage
) {
    public static StandingOrderResponse from(StandingOrder so) {
        return new StandingOrderResponse(
                so.getId(),
                so.getSourceIban(),
                so.getDestinationIban(),
                so.getAmount(),
                so.getCronExpression(),
                so.isActive(),
                so.getCreatedAt(),
                so.getLastExecutedAt(),
                so.getLastFailureMessage()
        );
    }
}
