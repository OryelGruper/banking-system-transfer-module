package com.bankingsystem.transfer.web;

import com.bankingsystem.transfer.domain.EntryType;
import com.bankingsystem.transfer.dto.LedgerEntryResponse;
import com.bankingsystem.transfer.service.LedgerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping
    public Page<LedgerEntryResponse> query(
            @RequestParam(required = false) String iban,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(required = false) EntryType type,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @PageableDefault(size = 20) Pageable pageable) {

        Instant from = dateFrom != null ? dateFrom.toInstant() : null;
        Instant to = dateTo != null ? dateTo.toInstant() : null;
        return ledgerService.query(iban, from, to, type, minAmount, maxAmount, pageable);
    }
}
