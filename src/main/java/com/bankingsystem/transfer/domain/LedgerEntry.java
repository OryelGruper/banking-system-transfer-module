package com.bankingsystem.transfer.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single append-only bookkeeping entry. The ledger is never updated or
 * deleted -- corrections happen by posting new, opposite entries, never by
 * mutating history.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "transfer_id", nullable = false)
    private String transferId;

    @Column(name = "account_iban", nullable = false)
    private String accountIban;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", length = 6, nullable = false)
    private EntryType entryType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 3, nullable = false)
    private Currency currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {
        // JPA
    }

    public LedgerEntry(String id, String transferId, String accountIban, EntryType entryType,
                        BigDecimal amount, Currency currency, Instant createdAt) {
        this.id = id;
        this.transferId = transferId;
        this.accountIban = accountIban;
        this.entryType = entryType;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getAccountIban() {
        return accountIban;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
