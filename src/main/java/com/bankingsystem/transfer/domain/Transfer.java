package com.bankingsystem.transfer.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One executed transfer (one-time or from a standing order). Every
 * successful transfer also produces two {@link LedgerEntry} rows: a
 * DEBIT on the source and a CREDIT on the destination.
 */
@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "source_iban", nullable = false)
    private String sourceIban;

    @Column(name = "destination_iban", nullable = false)
    private String destinationIban;

    @Column(name = "source_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal sourceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_currency", length = 3, nullable = false)
    private Currency sourceCurrency;

    @Column(name = "destination_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal destinationAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_currency", length = 3, nullable = false)
    private Currency destinationCurrency;

    @Column(name = "fx_rate_applied", precision = 19, scale = 6)
    private BigDecimal fxRateApplied;

    @Column(name = "standing_order_id", length = 36)
    private String standingOrderId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Transfer() {
        // JPA
    }

    public Transfer(String id, String sourceIban, String destinationIban,
                     BigDecimal sourceAmount, Currency sourceCurrency,
                     BigDecimal destinationAmount, Currency destinationCurrency,
                     BigDecimal fxRateApplied, String standingOrderId,
                     String correlationId, Instant createdAt) {
        this.id = id;
        this.sourceIban = sourceIban;
        this.destinationIban = destinationIban;
        this.sourceAmount = sourceAmount;
        this.sourceCurrency = sourceCurrency;
        this.destinationAmount = destinationAmount;
        this.destinationCurrency = destinationCurrency;
        this.fxRateApplied = fxRateApplied;
        this.standingOrderId = standingOrderId;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getSourceIban() {
        return sourceIban;
    }

    public String getDestinationIban() {
        return destinationIban;
    }

    public BigDecimal getSourceAmount() {
        return sourceAmount;
    }

    public Currency getSourceCurrency() {
        return sourceCurrency;
    }

    public BigDecimal getDestinationAmount() {
        return destinationAmount;
    }

    public Currency getDestinationCurrency() {
        return destinationCurrency;
    }

    public BigDecimal getFxRateApplied() {
        return fxRateApplied;
    }

    public String getStandingOrderId() {
        return standingOrderId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
