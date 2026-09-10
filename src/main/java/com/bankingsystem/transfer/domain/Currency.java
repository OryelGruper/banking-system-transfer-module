package com.bankingsystem.transfer.domain;

/**
 * Currencies this module supports today.
 * FxRateProvider isn't limited to a pair -- adding one more is a config
 * change, not a code change.
 */
public enum Currency {
    USD,
    EUR
}
