package com.example.transaction.repository.impl;

import com.example.transaction.exception.PageOutOfRangeException;
import com.example.transaction.exception.TransactionTooManyException;
import com.example.transaction.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTransactionRepositoryTest {

    private InMemoryTransactionRepository repository;
    private static final String TEST_USER = "testUser";
    private static final String TEST_TO_USER = "testToUser";

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
    }

    @Test
    void save_ShouldSaveTransactionSuccessfully() {
        // 准备测试数据
        Transaction transaction = createTestTransaction(1L);

        // 执行测试
        Transaction saved = repository.save(transaction);

        // 验证结果
        assertNotNull(saved);
        assertEquals(transaction.getId(), saved.getId());
        assertEquals(transaction.getUserName(), saved.getUserName());
        assertEquals(transaction.getAmount(), saved.getAmount());
        assertEquals(transaction.getType(), saved.getType());
    }

    @Test
    void save_ShouldThrowException_WhenTooManyTransactions() {
        // 准备测试数据
        int maxSize = 100;
        repository.setMaxSize(100);
        for (long i = 0; i <= maxSize + 1; i++) {
            Transaction transaction = createTestTransaction(i);
            if (i == maxSize + 1) {
                assertThrows(TransactionTooManyException.class, () -> repository.save(transaction));
            } else {
                repository.save(transaction);
            }
        }
    }

    @Test
    void findByUserNameAndId_ShouldReturnTransaction_WhenExists() {
        // 准备测试数据
        Transaction transaction = createTestTransaction(1L);
        repository.save(transaction);

        // 执行测试
        Optional<Transaction> found = repository.findByUserNameAndId(TEST_USER, 1L);

        // 验证结果
        assertTrue(found.isPresent());
        assertEquals(transaction.getId(), found.get().getId());
        assertEquals(transaction.getUserName(), found.get().getUserName());
    }

    @Test
    void findByUserNameAndId_ShouldReturnEmpty_WhenNotExists() {
        // 执行测试
        Optional<Transaction> found = repository.findByUserNameAndId(TEST_USER, 1L);

        // 验证结果
        assertFalse(found.isPresent());
    }

    @Test
    void deleteByUserNameAndId_ShouldDeleteTransaction_WhenExists() {
        // 准备测试数据
        Transaction transaction = createTestTransaction(1L);
        repository.save(transaction);

        // 执行测试
        Optional<Transaction> deleted = repository.deleteByUserNameAndId(TEST_USER, 1L);

        // 验证结果
        assertTrue(deleted.isPresent());
        assertEquals(transaction.getId(), deleted.get().getId());

        // 验证交易已被删除
        Optional<Transaction> found = repository.findByUserNameAndId(TEST_USER, 1L);
        assertFalse(found.isPresent());
    }

    @Test
    void deleteByUserNameAndId_ShouldReturnEmpty_WhenNotExists() {
        // 执行测试
        Optional<Transaction> deleted = repository.deleteByUserNameAndId(TEST_USER, 1L);

        // 验证结果
        assertFalse(deleted.isPresent());
    }

    @Test
    void findAllByUserName_ShouldReturnPagedResults() {
        // 准备测试数据
        for (long i = 1; i <= 5; i++) {
            repository.save(createTestTransaction(i));
        }

        // 执行测试
        Pageable pageable = PageRequest.of(0, 3);
        Page<Transaction> page = repository.findAllByUserName(TEST_USER, pageable);

        // 验证结果
        assertEquals(3, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(0, page.getNumber());
        assertEquals(3, page.getSize());
    }

    @Test
    void findAllByUserName_ShouldReturnEmptyPage_WhenNoTransactions() {
        // 执行测试
        Pageable pageable = PageRequest.of(0, 3);
        Page<Transaction> page = repository.findAllByUserName(TEST_USER, pageable);

        // 验证结果
        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void findAllByUserName_ShouldThrowException_WhenPageOutOfRange() {
        // 准备测试数据
        for (long i = 1; i <= 5; i++) {
            repository.save(createTestTransaction(i));
        }

        // 执行测试和验证
        Pageable pageable = PageRequest.of(2, 3);
        assertThrows(PageOutOfRangeException.class, () -> repository.findAllByUserName(TEST_USER, pageable));
    }

    @Test
    void findLastByUserName_ShouldReturnLastTransaction() {
        // 准备测试数据
        for (long i = 1; i <= 3; i++) {
            repository.save(createTestTransaction(i));
        }

        // 执行测试
        Optional<Transaction> last = repository.findLastByUserName(TEST_USER);

        // 验证结果
        assertTrue(last.isPresent());
        assertEquals(3L, last.get().getId());
    }

    @Test
    void findLastByUserName_ShouldReturnEmpty_WhenNoTransactions() {
        // 执行测试
        Optional<Transaction> last = repository.findLastByUserName(TEST_USER);

        // 验证结果
        assertFalse(last.isPresent());
    }

    private Transaction createTestTransaction(long id) {
        return Transaction.builder()
                .id(id)
                .userName(TEST_USER)
                .toUserName(TEST_TO_USER)
                .amount(new BigDecimal("100.00"))
                .type(Transaction.TransactionType.DEPOSIT)
                .description("测试交易")
                .createTimestamp(System.currentTimeMillis())
                .updateTimestamp(System.currentTimeMillis())
                .build();
    }
}