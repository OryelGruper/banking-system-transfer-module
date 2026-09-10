package com.bankingsystem.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StandingOrderRequest(

        @NotBlank(message = "sourceIban is required")
        String sourceIban,

        @NotBlank(message = "destinationIban is required")
        String destinationIban,

        @NotNull(message = "amount is required")
        BigDecimal amount,

        @NotBlank(message = "cronExpression is required")
        String cronExpression
) {
}
