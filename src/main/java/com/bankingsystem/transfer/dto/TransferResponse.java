package com.bankingsystem.transfer.dto;

import com.bankingsystem.transfer.domain.Currency;
import com.bankingsystem.transfer.domain.Transfer;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        String id,
        String sourceIban,
        String destinationIban,
        BigDecimal sourceAmount,
        Currency sourceCurrency,
        BigDecimal destinationAmount,
        Currency destinationCurrency,
        BigDecimal fxRateApplied,
        Instant createdAt
) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceIban(),
                transfer.getDestinationIban(),
                transfer.getSourceAmount(),
                transfer.getSourceCurrency(),
                transfer.getDestinationAmount(),
                transfer.getDestinationCurrency(),
                transfer.getFxRateApplied(),
                transfer.getCreatedAt()
        );
    }
}
