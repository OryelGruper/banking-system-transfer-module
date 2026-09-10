package com.bankingsystem.transfer.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(name = "iban", length = 34, nullable = false)
    private String iban;

    @Column(name = "owner", nullable = false)
    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 3, nullable = false)
    private Currency currency;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /**
     * Backup safety net -- the real concurrency control is the row lock in
     * AccountRepository#findByIbanForUpdate. This just catches any future
     * code path that updates a balance without taking that lock.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Account() {
        // JPA
    }

    public Account(String iban, String owner, Currency currency, BigDecimal balance) {
        this.iban = iban;
        this.owner = owner;
        this.currency = currency;
        this.balance = balance;
    }

    public String getIban() {
        return iban;
    }

    public String getOwner() {
        return owner;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public Long getVersion() {
        return version;
    }
}
