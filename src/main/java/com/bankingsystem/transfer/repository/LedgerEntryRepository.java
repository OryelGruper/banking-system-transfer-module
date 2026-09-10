package com.bankingsystem.transfer.repository;

import com.bankingsystem.transfer.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, String>, JpaSpecificationExecutor<LedgerEntry> {

    /** Sum of DEBIT amounts for an account in [start, end) -- used for the daily transfer limit. */
    @Query("""
            select coalesce(sum(l.amount), 0) from LedgerEntry l
            where l.accountIban = :iban
              and l.entryType = com.bankingsystem.transfer.domain.EntryType.DEBIT
              and l.createdAt >= :start and l.createdAt < :end
            """)
    BigDecimal sumDebitsForAccountBetween(@Param("iban") String iban,
                                           @Param("start") Instant start,
                                           @Param("end") Instant end);
}
