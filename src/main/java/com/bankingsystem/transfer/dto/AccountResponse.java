package com.bankingsystem.transfer.dto;

import com.bankingsystem.transfer.domain.Account;
import com.bankingsystem.transfer.domain.Currency;

import java.math.BigDecimal;

public record AccountResponse(
        String iban,
        String owner,
        Currency currency,
        BigDecimal balance
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getIban(), account.getOwner(), account.getCurrency(), account.getBalance());
    }
}
