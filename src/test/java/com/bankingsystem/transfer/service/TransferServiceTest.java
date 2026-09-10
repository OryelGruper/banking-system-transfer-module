package com.bankingsystem.transfer.service;

import com.bankingsystem.transfer.config.AppProperties;
import com.bankingsystem.transfer.domain.Account;
import com.bankingsystem.transfer.domain.Currency;
import com.bankingsystem.transfer.dto.TransferRequest;
import com.bankingsystem.transfer.dto.TransferResponse;
import com.bankingsystem.transfer.exception.DailyLimitExceededException;
import com.bankingsystem.transfer.exception.InsufficientFundsException;
import com.bankingsystem.transfer.repository.AccountRepository;
import com.bankingsystem.transfer.repository.LedgerEntryRepository;
import com.bankingsystem.transfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the core transfer business rules, covering the five
 * scenarios called out explicitly by the task spec. The repositories are
 * mocked; {@link FxRateProvider} is used as a real instance since its
 * logic is pure and simple enough to exercise directly.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private IdempotencyService idempotencyService;

    private AppProperties properties;
    private FxRateProvider fxRateProvider;
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        properties = new AppProperties(
                new AppProperties.Security("test-key"),
                new AppProperties.Fx(Map.of("EUR", new BigDecimal("0.86"))),
                new AppProperties.Transfer(new BigDecimal("20000")),
                new AppProperties.Idempotency(24),
                new AppProperties.StandingOrder(60000)
        );
        fxRateProvider = new FxRateProvider(properties);
        transferService = new TransferService(
                accountRepository, transferRepository, ledgerEntryRepository,
                idempotencyService, fxRateProvider, properties);

        // lenient(): not every test reaches the save calls (e.g. the
        // insufficient-funds and daily-limit tests throw before saving, and
        // the duplicate-idempotency-key test never touches these repositories
        // at all) -- Mockito's strict stubbing would otherwise fail those
        // tests for "unnecessary" stubbing.
        lenient().when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(transferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(ledgerEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void successfulSameCurrencyTransfer_debitsSourceAndCreditsDestination() {
        Account source = new Account("BG01FINV001", "Ivan Petrov", Currency.USD, new BigDecimal("10000.00"));
        Account destination = new Account("BG01FINV003", "Georgi Ivanov", Currency.USD, new BigDecimal("2500.00"));
        givenAccounts(source, destination);
        when(idempotencyService.findExisting(anyString())).thenReturn(Optional.empty());
        when(ledgerEntryRepository.sumDebitsForAccountBetween(eq("BG01FINV001"), any(), any())).thenReturn(BigDecimal.ZERO);

        TransferResponse response = transferService.execute(
                new TransferRequest("BG01FINV001", "BG01FINV003", new BigDecimal("100.00")),
                UUID.randomUUID().toString());

        assertThat(response.sourceAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.destinationAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(source.getBalance()).isEqualByComparingTo(new BigDecimal("9900.00"));
        assertThat(destination.getBalance()).isEqualByComparingTo(new BigDecimal("2600.00"));
        verify(idempotencyService).store(anyString(), eq("/api/v1/transfers"), any());
    }

    @Test
    void successfulCrossCurrencyTransfer_appliesFxRate() {
        Account source = new Account("BG01FINV001", "Ivan Petrov", Currency.USD, new BigDecimal("10000.00"));
        Account destination = new Account("BG01FINV002", "Maria Koleva", Currency.EUR, new BigDecimal("5000.00"));
        givenAccounts(source, destination);
        when(idempotencyService.findExisting(anyString())).thenReturn(Optional.empty());
        when(ledgerEntryRepository.sumDebitsForAccountBetween(eq("BG01FINV001"), any(), any())).thenReturn(BigDecimal.ZERO);

        TransferResponse response = transferService.execute(
                new TransferRequest("BG01FINV001", "BG01FINV002", new BigDecimal("100.00")),
                UUID.randomUUID().toString());

        // 100 USD * 0.86 = 86.00 EUR
        assertThat(response.destinationAmount()).isEqualByComparingTo(new BigDecimal("86.00"));
        assertThat(response.fxRateApplied()).isEqualByComparingTo(new BigDecimal("0.86"));
        assertThat(source.getBalance()).isEqualByComparingTo(new BigDecimal("9900.00"));
        assertThat(destination.getBalance()).isEqualByComparingTo(new BigDecimal("5086.00"));
    }

    @Test
    void insufficientFunds_throwsInsufficientFundsException() {
        Account source = new Account("BG01FINV003", "Georgi Ivanov", Currency.USD, new BigDecimal("2500.00"));
        Account destination = new Account("BG01FINV001", "Ivan Petrov", Currency.USD, new BigDecimal("10000.00"));
        givenAccounts(source, destination);
        when(idempotencyService.findExisting(anyString())).thenReturn(Optional.empty());
        when(ledgerEntryRepository.sumDebitsForAccountBetween(eq("BG01FINV003"), any(), any())).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> transferService.execute(
                new TransferRequest("BG01FINV003", "BG01FINV001", new BigDecimal("5000.00")),
                UUID.randomUUID().toString()))
                .isInstanceOf(InsufficientFundsException.class);

        verifyNoInteractions(transferRepository);
    }

    @Test
    void dailyLimitExceeded_throwsDailyLimitExceededException() {
        Account source = new Account("BG01FINV001", "Ivan Petrov", Currency.USD, new BigDecimal("10000.00"));
        Account destination = new Account("BG01FINV003", "Georgi Ivanov", Currency.USD, new BigDecimal("2500.00"));
        givenAccounts(source, destination);
        when(idempotencyService.findExisting(anyString())).thenReturn(Optional.empty());
        // Already moved 19,950 today; one more 100 would breach the 20,000 daily limit.
        when(ledgerEntryRepository.sumDebitsForAccountBetween(eq("BG01FINV001"), any(), any()))
                .thenReturn(new BigDecimal("19950.00"));

        assertThatThrownBy(() -> transferService.execute(
                new TransferRequest("BG01FINV001", "BG01FINV003", new BigDecimal("100.00")),
                UUID.randomUUID().toString()))
                .isInstanceOf(DailyLimitExceededException.class);

        verifyNoInteractions(transferRepository);
    }

    @Test
    void duplicateIdempotencyKey_returnsOriginalResponseWithoutReexecuting() {
        TransferResponse original = new TransferResponse(
                "existing-id", "BG01FINV001", "BG01FINV003",
                new BigDecimal("100.00"), Currency.USD,
                new BigDecimal("100.00"), Currency.USD,
                null, Instant.now());
        String key = UUID.randomUUID().toString();
        when(idempotencyService.findExisting(key)).thenReturn(Optional.of(original));

        TransferResponse response = transferService.execute(
                new TransferRequest("BG01FINV001", "BG01FINV003", new BigDecimal("100.00")), key);

        assertThat(response).isEqualTo(original);
        verifyNoInteractions(accountRepository);
        verifyNoInteractions(transferRepository);
        verifyNoInteractions(ledgerEntryRepository);
        verify(idempotencyService, never()).store(any(), any(), any());
    }

    private void givenAccounts(Account source, Account destination) {
        when(accountRepository.findByIbanForUpdate(source.getIban())).thenReturn(Optional.of(source));
        when(accountRepository.findByIbanForUpdate(destination.getIban())).thenReturn(Optional.of(destination));
    }
}
