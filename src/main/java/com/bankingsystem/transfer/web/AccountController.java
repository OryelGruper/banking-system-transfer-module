package com.bankingsystem.transfer.web;

import com.bankingsystem.transfer.dto.AccountResponse;
import com.bankingsystem.transfer.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountResponse> listAll() {
        return accountService.listAccounts();
    }

    @GetMapping("/{iban}")
    public AccountResponse getByIban(@PathVariable String iban) {
        return accountService.getAccount(iban);
    }
}
