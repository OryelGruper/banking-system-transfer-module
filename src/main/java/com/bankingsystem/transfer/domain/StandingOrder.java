package com.bankingsystem.transfer.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "standing_orders")
public class StandingOrder {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "source_iban", nullable = false)
    private String sourceIban;

    @Column(name = "destination_iban", nullable = false)
    private String destinationIban;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "cron_expression", nullable = false, length = 64)
    private String cronExpression;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;

    @Column(name = "last_failure_message", length = 500)
    private String lastFailureMessage;

    protected StandingOrder() {
        // JPA
    }

    public StandingOrder(String id, String sourceIban, String destinationIban, BigDecimal amount,
                          String cronExpression, Instant createdAt) {
        this.id = id;
        this.sourceIban = sourceIban;
        this.destinationIban = destinationIban;
        this.amount = amount;
        this.cronExpression = cronExpression;
        this.active = true;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public boolean isActive() {
        return active;
    }

    public void cancel() {
        this.active = false;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void recordSuccess(Instant executedAt) {
        this.lastExecutedAt = executedAt;
        this.lastFailureMessage = null;
    }

    public void recordFailure(String message) {
        // Doesn't advance lastExecutedAt, so the poller retries this order
        // next run instead of skipping it.
        this.lastFailureMessage = message;
    }

    public String getLastFailureMessage() {
        return lastFailureMessage;
    }
}
