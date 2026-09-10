package com.bankingsystem.transfer.dto;

import com.bankingsystem.transfer.domain.Currency;
import com.bankingsystem.transfer.domain.EntryType;
import com.bankingsystem.transfer.domain.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(
        String id,
        String transferId,
        String accountIban,
        EntryType entryType,
        BigDecimal amount,
        Currency currency,
        Instant createdAt
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransferId(),
                entry.getAccountIban(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getCurrency(),
                entry.getCreatedAt()
        );
    }
}
