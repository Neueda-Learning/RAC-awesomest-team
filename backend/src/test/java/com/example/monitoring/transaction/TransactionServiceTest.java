package com.example.monitoring.transaction;

import com.example.monitoring.alert.repository.AlertRepository;
import com.example.monitoring.rule.service.RuleEngineService;
import com.example.monitoring.transaction.dto.CreateTransactionRequest;
import com.example.monitoring.transaction.dto.GenerateTransactionsRequest;
import com.example.monitoring.transaction.entity.Transaction;
import com.example.monitoring.transaction.repository.TransactionRepository;
import com.example.monitoring.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RuleEngineService ruleEngineService;

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void createTransaction_shouldPersistAndTriggerRuleEngine() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId("ACC-001");
        request.setPayeeId("PAY-001");
        request.setAmount(new BigDecimal("123.45"));
        request.setCurrency(null);
        request.setTransactionType("TRANSFER_OUT");
        request.setDescription("test tx");

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(1L);
            return tx;
        });
        when(alertRepository.existsByTransactionId(1L)).thenReturn(true);

        Transaction result = transactionService.createTransaction(request);

        assertEquals(1L, result.getId());
        assertEquals("USD", result.getCurrency());
        assertNotNull(result.getCreatedAt());
        assertTrue(Boolean.TRUE.equals(result.getAlertTriggered()));
        verify(ruleEngineService).evaluate(result);
        verify(alertRepository).existsByTransactionId(1L);
    }

    @Test
    void filterTransactions_shouldUseCombinedQueryWhenAmountAndDateProvided() {
        BigDecimal min = new BigDecimal("100");
        BigDecimal max = new BigDecimal("1000");
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();

        transactionService.filterTransactions(min, max, from, to);

        // After update: filterTransactions now calls findByCreatedAtBetween for date filtering,
        // then applies FX-based USD conversion for amount filtering
        verify(transactionRepository).findByCreatedAtBetween(from, to);
    }

    @Test
    void filterTransactions_shouldThrowWhenAmountRangeInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.filterTransactions(new BigDecimal("200"), new BigDecimal("100"), null, null));
    }

    @Test
    void generateMockTransactions_shouldCreateExpectedCount() {
        AtomicLong idGenerator = new AtomicLong(1L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(idGenerator.getAndIncrement());
            return tx;
        });

        GenerateTransactionsRequest request = new GenerateTransactionsRequest();
        request.setCount(3);
        List<Transaction> generated = transactionService.generateMockTransactions(request);

        assertEquals(3, generated.size());
        verify(transactionRepository, times(3)).save(any(Transaction.class));
        verify(ruleEngineService, times(3)).evaluate(any(Transaction.class));
        verify(alertRepository, times(3)).existsByTransactionId(any(Long.class));
    }

    @Test
    void generateMockTransactions_shouldRejectNonPositiveCount() {
        GenerateTransactionsRequest request = new GenerateTransactionsRequest();
        request.setCount(0);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.generateMockTransactions(request));
        assertEquals("count must be greater than 0", ex.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(ruleEngineService, never()).evaluate(any(Transaction.class));
    }

    @Test
    void generateMockTransactions_shouldRespectAmountRangeAndCreatedAtStep() {
        AtomicLong idGenerator = new AtomicLong(1L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(idGenerator.getAndIncrement());
            return tx;
        });

        GenerateTransactionsRequest request = new GenerateTransactionsRequest();
        request.setCount(3);
        request.setMinAmount(new BigDecimal("100.00"));
        request.setMaxAmount(new BigDecimal("150.00"));
        request.setStartAt(LocalDateTime.of(2026, 7, 28, 10, 0, 0));
        request.setStepSeconds(120);

        transactionService.generateMockTransactions(request);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(3)).save(captor.capture());
        List<Transaction> saved = captor.getAllValues();

        assertEquals(LocalDateTime.of(2026, 7, 28, 10, 0, 0), saved.get(0).getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 7, 28, 10, 2, 0), saved.get(1).getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 7, 28, 10, 4, 0), saved.get(2).getCreatedAt());
        saved.forEach(tx -> {
            assertTrue(tx.getAmount().compareTo(new BigDecimal("100.00")) >= 0);
            assertTrue(tx.getAmount().compareTo(new BigDecimal("150.00")) <= 0);
        });
    }

    @Test
    void generateMockTransactions_shouldSpreadCreatedAtAcrossRangeWhenNoStepProvided() {
        AtomicLong idGenerator = new AtomicLong(1L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(idGenerator.getAndIncrement());
            return tx;
        });

        GenerateTransactionsRequest request = new GenerateTransactionsRequest();
        request.setCount(3);
        request.setStartAt(LocalDateTime.of(2026, 7, 28, 8, 0, 0));
        request.setEndAt(LocalDateTime.of(2026, 7, 28, 8, 10, 0));

        transactionService.generateMockTransactions(request);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(3)).save(captor.capture());
        List<Transaction> saved = captor.getAllValues();

        assertEquals(LocalDateTime.of(2026, 7, 28, 8, 0, 0), saved.get(0).getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 7, 28, 8, 5, 0), saved.get(1).getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 7, 28, 8, 10, 0), saved.get(2).getCreatedAt());
        assertFalse(saved.get(0).getCreatedAt().equals(saved.get(1).getCreatedAt()));
    }

    @Test
    void generateMockTransactions_shouldRejectInvalidTimeRange() {
        GenerateTransactionsRequest request = new GenerateTransactionsRequest();
        request.setCount(5);
        request.setStartAt(LocalDateTime.of(2026, 7, 28, 10, 0, 0));
        request.setEndAt(LocalDateTime.of(2026, 7, 28, 10, 1, 0));
        request.setStepSeconds(30);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.generateMockTransactions(request));
        assertEquals("time range must be large enough for count and stepSeconds", ex.getMessage());
    }
}