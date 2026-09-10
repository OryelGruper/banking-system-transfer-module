package com.bankingsystem.transfer.service;

import com.bankingsystem.transfer.domain.EntryType;
import com.bankingsystem.transfer.domain.LedgerEntry;
import com.bankingsystem.transfer.dto.LedgerEntryResponse;
import com.bankingsystem.transfer.repository.LedgerEntryRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * All filters are optional and combined with AND, per the spec.
     */
    public Page<LedgerEntryResponse> query(String accountIban, Instant dateFrom, Instant dateTo,
                                            EntryType type, BigDecimal minAmount, BigDecimal maxAmount,
                                            Pageable pageable) {
        Specification<LedgerEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (accountIban != null && !accountIban.isBlank()) {
                predicates.add(cb.equal(root.get("accountIban"), accountIban));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("entryType"), type));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return ledgerEntryRepository.findAll(spec, pageable).map(LedgerEntryResponse::from);
    }
}
