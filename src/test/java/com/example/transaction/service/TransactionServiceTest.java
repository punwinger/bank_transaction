package com.example.transaction.service;

import com.example.transaction.dto.TransactionRequest;
import com.example.transaction.exception.TransactionNotFoundException;
import com.example.transaction.model.Transaction;
import com.example.transaction.repository.TransactionRepository;
import com.example.transaction.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionService transactionService;
    private TransactionRequest sampleRequest;
    private static final String TEST_USER = "testUser";
    private static final String TEST_TO_USER = "testToUser";

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(transactionRepository);
        sampleRequest = new TransactionRequest();
        sampleRequest.setUserName(TEST_USER);
        sampleRequest.setAmount(new BigDecimal("100.00"));
        sampleRequest.setType(Transaction.TransactionType.DEPOSIT);
        sampleRequest.setDescription("测试交易");
    }

    @Test
    void createTransaction_ShouldCreateSuccessfully() {
        // 准备测试数据
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 执行测试
        Transaction transaction = transactionService.createTransaction(sampleRequest);

        // 验证结果
        assertNotNull(transaction);
        assertNotNull(transaction.getId());
        assertEquals(TEST_USER, transaction.getUserName());
        assertEquals(sampleRequest.getAmount(), transaction.getAmount());
        assertEquals(sampleRequest.getType(), transaction.getType());
        assertEquals(sampleRequest.getDescription(), transaction.getDescription());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransferTransaction_ShouldCreateSuccessfully() {
        // 准备转账请求
        sampleRequest.setType(Transaction.TransactionType.TRANSFER);
        sampleRequest.setToUserName(TEST_TO_USER);
        
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 执行测试
        Transaction transaction = transactionService.createTransaction(sampleRequest);

        // 验证结果
        assertNotNull(transaction);
        assertEquals(TEST_USER, transaction.getUserName());
        assertEquals(TEST_TO_USER, transaction.getToUserName());
        assertEquals(Transaction.TransactionType.TRANSFER, transaction.getType());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_ShouldUpdateSuccessfully() {
        // 准备测试数据
        String transactionId = "test-id";
        Transaction existingTransaction = Transaction.builder()
                .id(transactionId)
                .userName(TEST_USER)
                .amount(new BigDecimal("100.00"))
                .type(Transaction.TransactionType.DEPOSIT)
                .description("原始交易")
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionRepository.findByUserNameAndId(TEST_USER, transactionId))
                .thenReturn(Optional.of(existingTransaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 准备更新请求
        TransactionRequest updateRequest = new TransactionRequest();
        updateRequest.setUserName(TEST_USER);
        updateRequest.setAmount(new BigDecimal("200.00"));
        updateRequest.setType(Transaction.TransactionType.WITHDRAWAL);
        updateRequest.setDescription("更新的测试交易");

        // 执行测试
        Transaction updated = transactionService.updateTransaction(TEST_USER, transactionId, updateRequest);

        // 验证结果
        assertNotNull(updated);
        assertEquals(transactionId, updated.getId());
        assertEquals(TEST_USER, updated.getUserName());
        assertEquals(updateRequest.getAmount(), updated.getAmount());
        assertEquals(updateRequest.getType(), updated.getType());
        assertEquals(updateRequest.getDescription(), updated.getDescription());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void deleteTransaction_ShouldDeleteSuccessfully() {
        // 准备测试数据
        String transactionId = "test-id";
        Transaction existingTransaction = Transaction.builder()
                .id(transactionId)
                .userName(TEST_USER)
                .build();

        when(transactionRepository.findByUserNameAndId(TEST_USER, transactionId))
                .thenReturn(Optional.of(existingTransaction));

        // 执行测试
        assertDoesNotThrow(() -> transactionService.deleteTransaction(TEST_USER, transactionId));

        // 验证结果
        verify(transactionRepository).deleteByUserNameAndId(TEST_USER, transactionId);
    }

    @Test
    void deleteTransaction_ShouldThrowException_WhenNotFound() {
        // 准备测试数据
        String transactionId = "non-existent-id";
        when(transactionRepository.findByUserNameAndId(TEST_USER, transactionId))
                .thenReturn(Optional.empty());

        // 执行测试和验证
        assertThrows(TransactionNotFoundException.class, 
            () -> transactionService.deleteTransaction(TEST_USER, transactionId));
    }

    @Test
    void getTransaction_ShouldReturnTransaction() {
        // 准备测试数据
        String transactionId = "test-id";
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .userName(TEST_USER)
                .build();

        when(transactionRepository.findByUserNameAndId(TEST_USER, transactionId))
                .thenReturn(Optional.of(transaction));

        // 执行测试
        Transaction found = transactionService.getTransaction(TEST_USER, transactionId);

        // 验证结果
        assertNotNull(found);
        assertEquals(transactionId, found.getId());
        assertEquals(TEST_USER, found.getUserName());
    }

    @Test
    void getTransaction_ShouldThrowException_WhenNotFound() {
        // 准备测试数据
        String transactionId = "non-existent-id";
        when(transactionRepository.findByUserNameAndId(TEST_USER, transactionId))
                .thenReturn(Optional.empty());

        // 执行测试和验证
        assertThrows(TransactionNotFoundException.class, 
            () -> transactionService.getTransaction(TEST_USER, transactionId));
    }

    @Test
    void getAllTransactions_ShouldReturnPagedResults() {
        // 准备测试数据
        PageRequest pageRequest = PageRequest.of(0, 3);
        Page<Transaction> expectedPage = new PageImpl<>(
            Arrays.asList(
                Transaction.builder().id("1").userName(TEST_USER).build(),
                Transaction.builder().id("2").userName(TEST_USER).build(),
                Transaction.builder().id("3").userName(TEST_USER).build()
            ),
            pageRequest,
            5
        );

        when(transactionRepository.findAllByUserName(TEST_USER, pageRequest))
                .thenReturn(expectedPage);

        // 执行测试
        Page<Transaction> result = transactionService.getAllTransactions(TEST_USER, pageRequest);

        // 验证结果
        assertEquals(3, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(0, result.getNumber());
        assertEquals(3, result.getSize());
    }

    @Test
    void createTransaction_ShouldThrowException_WhenInvalidAmount() {
        // 准备无效金额的请求
        sampleRequest.setAmount(new BigDecimal("-100.00"));

        // 执行测试和验证
        assertThrows(IllegalArgumentException.class,
            () -> transactionService.createTransaction(sampleRequest));
    }

    @Test
    void createTransaction_ShouldThrowException_WhenInvalidDescription() {
        // 准备过长描述的请求
        sampleRequest.setDescription("这是一个超过二十个字符的非常长的交易描述信息测试");

        // 执行测试和验证
        assertThrows(IllegalArgumentException.class,
            () -> transactionService.createTransaction(sampleRequest));
    }
} 