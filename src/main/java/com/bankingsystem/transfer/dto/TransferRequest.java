package com.bankingsystem.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * amount isn't range-checked here on purpose: "amount &lt;= 0" is a
 * business-rule failure per the spec, so it's checked in the service
 * layer and reported as 422, same as insufficient funds and the daily
 * limit -- not as a generic 400.
 */
public record TransferRequest(

        @NotBlank(message = "sourceIban is required")
        String sourceIban,

        @NotBlank(message = "destinationIban is required")
        String destinationIban,

        @NotNull(message = "amount is required")
        BigDecimal amount
) {
}
