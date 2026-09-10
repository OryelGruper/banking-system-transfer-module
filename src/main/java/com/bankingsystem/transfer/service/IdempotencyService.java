package com.bankingsystem.transfer.service;

import tools.jackson.databind.json.JsonMapper;
import com.bankingsystem.transfer.config.AppProperties;
import com.bankingsystem.transfer.domain.IdempotencyRecord;
import com.bankingsystem.transfer.dto.TransferResponse;
import com.bankingsystem.transfer.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final AppProperties properties;
    private final JsonMapper jsonMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, AppProperties properties, JsonMapper jsonMapper) {
        this.repository = repository;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    /** The stored response for this key, if it hasn't expired (default TTL 24h) -- an expired key is treated as new. */
    public Optional<TransferResponse> findExisting(String idempotencyKey) {
        return repository.findById(idempotencyKey)
                .filter(this::notExpired)
                .map(this::deserialize);
    }

    public void store(String idempotencyKey, String requestPath, TransferResponse response) {
        String body = serialize(response);
        repository.save(new IdempotencyRecord(idempotencyKey, requestPath, body, Instant.now()));
    }

    private boolean notExpired(IdempotencyRecord record) {
        Instant expiresAt = record.getCreatedAt().plusSeconds(properties.idempotency().ttlHours() * 3600);
        return Instant.now().isBefore(expiresAt);
    }

    private String serialize(TransferResponse response) {
        try {
            return jsonMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize idempotent response", e);
        }
    }

    private TransferResponse deserialize(IdempotencyRecord record) {
        try {
            return jsonMapper.readValue(record.getResponseBody(), TransferResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize stored idempotent response", e);
        }
    }
}
