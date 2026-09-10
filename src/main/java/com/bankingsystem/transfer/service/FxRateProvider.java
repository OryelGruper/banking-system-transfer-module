package com.bankingsystem.transfer.service;

import com.bankingsystem.transfer.config.AppProperties;
import com.bankingsystem.transfer.domain.Currency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts between currencies using USD as a base: every configured rate
 * says how many units of that currency equal 1 USD, and converting
 * between two non-USD currencies hops through USD. Adding a currency is
 * just a new {@code app.fx.rates} entry (plus a {@link Currency} value)
 * -- no code change here.
 */
@Component
public class FxRateProvider {

    /** What every configured rate is measured against. Never itself a key in app.fx.rates. */
    private static final Currency BASE_CURRENCY = Currency.USD;
    private static final int CONVERSION_SCALE = 10;

    private final AppProperties properties;

    public FxRateProvider(AppProperties properties) {
        this.properties = properties;
    }

    /** Converts amount from one currency to another, rounded to 2 decimals. */
    public BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        if (from == to) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal rate = rateFrom(from, to);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /** The multiplier to go from {@code from} to {@code to}, or null if no conversion is needed. */
    public BigDecimal rateFrom(Currency from, Currency to) {
        if (from == to) {
            return null;
        }
        if (from == BASE_CURRENCY) {
            return rateToBase(to);
        }
        if (to == BASE_CURRENCY) {
            return BigDecimal.ONE.divide(rateToBase(from), CONVERSION_SCALE, RoundingMode.HALF_UP);
        }
        // Neither side is USD, so hop through it: from -> USD -> to.
        BigDecimal fromToBase = BigDecimal.ONE.divide(rateToBase(from), CONVERSION_SCALE, RoundingMode.HALF_UP);
        return fromToBase.multiply(rateToBase(to));
    }

    /** How many units of {@code currency} equal 1 unit of {@link #BASE_CURRENCY}. */
    private BigDecimal rateToBase(Currency currency) {
        if (currency == BASE_CURRENCY) {
            return BigDecimal.ONE;
        }
        BigDecimal rate = properties.fx().rates().get(currency.name());
        if (rate == null) {
            throw new IllegalStateException(
                    "No FX rate configured for currency " + currency
                            + " (expected app.fx.rates." + currency.name() + ")");
        }
        return rate;
    }
}
