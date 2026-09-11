package com.bankingsystem.transfer.service;

import com.bankingsystem.transfer.config.AppProperties;
import com.bankingsystem.transfer.domain.Account;
import com.bankingsystem.transfer.domain.Currency;
import com.bankingsystem.transfer.domain.EntryType;
import com.bankingsystem.transfer.domain.LedgerEntry;
import com.bankingsystem.transfer.domain.StandingOrder;
import com.bankingsystem.transfer.domain.Transfer;
import com.bankingsystem.transfer.dto.TransferRequest;
import com.bankingsystem.transfer.dto.TransferResponse;
import com.bankingsystem.transfer.exception.DailyLimitExceededException;
import com.bankingsystem.transfer.exception.InsufficientFundsException;
import com.bankingsystem.transfer.exception.InvalidTransferException;
import com.bankingsystem.transfer.exception.TransferNotFoundException;
import com.bankingsystem.transfer.repository.AccountRepository;
import com.bankingsystem.transfer.repository.LedgerEntryRepository;
import com.bankingsystem.transfer.repository.TransferRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Runs every transfer -- one-time ({@link #execute}) or from a standing
 * order ({@link #executeForStandingOrder}) -- through
 * {@link #performTransfer}, so the accounting rules only live in one place.
 */
@Service
public class TransferService {

    private static final Logger log = LogManager.getLogger(TransferService.class);
    private static final String TRANSFERS_PATH = "/api/v1/transfers";

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyService idempotencyService;
    private final FxRateProvider fxRateProvider;
    private final AppProperties properties;

    public TransferService(AccountRepository accountRepository,
                            TransferRepository transferRepository,
                            LedgerEntryRepository ledgerEntryRepository,
                            IdempotencyService idempotencyService,
                            FxRateProvider fxRateProvider,
                            AppProperties properties) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.idempotencyService = idempotencyService;
        this.fxRateProvider = fxRateProvider;
        this.properties = properties;
    }

    public TransferResponse getById(String id) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new TransferNotFoundException(id));
        return TransferResponse.from(transfer);
    }

    /** Entry point for the API: checks the idempotency key first, then hands off to {@link #performTransfer}. */
    @Transactional
    public TransferResponse execute(TransferRequest request, String idempotencyKey) {
        Optional<TransferResponse> existing = idempotencyService.findExisting(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotency key {} already processed, returning the stored response instead of re-executing", idempotencyKey);
            return existing.get();
        }

        Transfer transfer = performTransfer(request.sourceIban(), request.destinationIban(), request.amount(), null);
        TransferResponse response = TransferResponse.from(transfer);

        idempotencyService.store(idempotencyKey, TRANSFERS_PATH, response);
        return response;
    }

    /**
     * Called by the scheduler. Skips the idempotency table -- each firing is
     * a real transfer, not a retry -- but follows every other rule in
     * {@link #execute}.
     */
    @Transactional
    public TransferResponse executeForStandingOrder(StandingOrder order) {
        Transfer transfer = performTransfer(order.getSourceIban(), order.getDestinationIban(), order.getAmount(), order.getId());
        return TransferResponse.from(transfer);
    }

    private Transfer performTransfer(String sourceIban, String destinationIban, BigDecimal amount, String standingOrderId) {
        log.info("Transfer attempt: {} -> {} amount={} standingOrderId={}", sourceIban, destinationIban, amount, standingOrderId);
        try {
            if (amount == null || amount.signum() <= 0) {
                throw new InvalidTransferException("Transfer amount must be greater than zero");
            }
            if (sourceIban == null || destinationIban == null || sourceIban.equals(destinationIban)) {
                throw new InvalidTransferException("Source and destination accounts must be different");
            }

            // Always lock accounts in IBAN order, not source/destination
            // order, so two transfers crossing each other can't deadlock.
            String firstIban = sourceIban.compareTo(destinationIban) <= 0 ? sourceIban : destinationIban;
            String secondIban = firstIban.equals(sourceIban) ? destinationIban : sourceIban;

            Account firstLocked = accountRepository.findByIbanForUpdate(firstIban)
                    .orElseThrow(() -> new InvalidTransferException("Account does not exist: " + firstIban));
            Account secondLocked = accountRepository.findByIbanForUpdate(secondIban)
                    .orElseThrow(() -> new InvalidTransferException("Account does not exist: " + secondIban));

            Account source = firstIban.equals(sourceIban) ? firstLocked : secondLocked;
            Account destination = firstIban.equals(sourceIban) ? secondLocked : firstLocked;

            BigDecimal dailyLimit = properties.transfer().dailyLimit();
            BigDecimal alreadyTransferredToday = sumOfTodaysOutgoingTransfers(source.getIban());
            if (alreadyTransferredToday.add(amount).compareTo(dailyLimit) > 0) {
                throw new DailyLimitExceededException(
                        "Daily transfer limit of " + dailyLimit + " " + source.getCurrency() + " exceeded for account " + source.getIban());
            }

            if (source.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient funds in source account");
            }

            BigDecimal destinationAmount = fxRateProvider.convert(amount, source.getCurrency(), destination.getCurrency());
            BigDecimal fxRateApplied = fxRateProvider.rateFrom(source.getCurrency(), destination.getCurrency());

            source.debit(amount);
            destination.credit(destinationAmount);
            accountRepository.save(source);
            accountRepository.save(destination);

            Instant now = Instant.now();
            String transferId = UUID.randomUUID().toString();
            Transfer transfer = new Transfer(
                    transferId, source.getIban(), destination.getIban(),
                    amount, source.getCurrency(),
                    destinationAmount, destination.getCurrency(),
                    fxRateApplied, standingOrderId,
                    ThreadContext.get("correlationId"),
                    now
            );
            transferRepository.save(transfer);

            ledgerEntryRepository.save(new LedgerEntry(
                    UUID.randomUUID().toString(), transferId, source.getIban(),
                    EntryType.DEBIT, amount, source.getCurrency(), now));
            ledgerEntryRepository.save(new LedgerEntry(
                    UUID.randomUUID().toString(), transferId, destination.getIban(),
                    EntryType.CREDIT, destinationAmount, destination.getCurrency(), now));

            log.info("Transfer succeeded: id={} {} -> {} sourceAmount={} {} destinationAmount={} {}",
                    transferId, source.getIban(), destination.getIban(),
                    amount, source.getCurrency(), destinationAmount, destination.getCurrency());

            return transfer;
        } catch (RuntimeException ex) {
            log.warn("Transfer failed: {} -> {} amount={} reason={}", sourceIban, destinationIban, amount, ex.getMessage());
            throw ex;
        }
    }

    private BigDecimal sumOfTodaysOutgoingTransfers(String iban) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfNextDay = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return ledgerEntryRepository.sumDebitsForAccountBetween(iban, startOfDay, startOfNextDay);
    }
}
