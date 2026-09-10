package com.bankingsystem.transfer.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** The response body previously returned for an X-Idempotency-Key, so a retry with the same key doesn't re-run the transfer. */
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", length = 64, nullable = false)
    private String idempotencyKey;

    @Column(name = "request_path", nullable = false)
    private String requestPath;

    @Lob
    @Column(name = "response_body", nullable = false)
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
        // JPA
    }

    public IdempotencyRecord(String idempotencyKey, String requestPath, String responseBody, Instant createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.requestPath = requestPath;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
