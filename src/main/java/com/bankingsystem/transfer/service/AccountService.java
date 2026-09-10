package com.bankingsystem.transfer.service;

import com.bankingsystem.transfer.domain.Account;
import com.bankingsystem.transfer.dto.AccountResponse;
import com.bankingsystem.transfer.exception.AccountNotFoundException;
import com.bankingsystem.transfer.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<AccountResponse> listAccounts() {
        return accountRepository.findAll().stream()
                .map(AccountResponse::from)
                .toList();
    }

    public AccountResponse getAccount(String iban) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));
        return AccountResponse.from(account);
    }
}
