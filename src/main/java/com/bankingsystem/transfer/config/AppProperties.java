package com.bankingsystem.transfer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

/** Binds the app.* config tree -- every value comes from application.yml / env vars */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Security security,
        Fx fx,
        Transfer transfer,
        Idempotency idempotency,
        StandingOrder standingOrder
) {
    public record Security(String apiKey) {
    }

    public record Fx(Map<String, BigDecimal> rates) {
    }

    public record Transfer(BigDecimal dailyLimit) {
    }

    public record Idempotency(long ttlHours) {
    }

    public record StandingOrder(long pollIntervalMs) {
    }
}
