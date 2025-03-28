package com.example.transaction.service.impl;

import com.example.transaction.dto.TransactionRequest;
import com.example.transaction.exception.DuplicateTransactionException;
import com.example.transaction.exception.TransactionNotFoundException;
import com.example.transaction.model.Transaction;
import com.example.transaction.repository.TransactionRepository;
import com.example.transaction.service.TransactionService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final AtomicLong idGenerator = new AtomicLong(1);

    private final ConcurrentHashMap<String, ReentrantReadWriteLock> lockMap = new ConcurrentHashMap<>();

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction createTransaction(TransactionRequest request) {
        validateTransactionRequest(request);
        checkDuplicateTransaction(request);

        long id = idGenerator.getAndIncrement();
        long curTs = System.currentTimeMillis();
        Transaction transaction = Transaction.builder()
                .id(id)
                .userName(request.getUserName())
                .toUserName(request.getToUserName())
                .amount(request.getAmount())
                .type(request.getType())
                .description(request.getDescription())
                .createTimestamp(curTs)
                .updateTimestamp(curTs)
                .build();

        ReadWriteLock lock = getUserLock(request.getUserName());
        lock.writeLock().lock();
        try {
            return transactionRepository.save(transaction);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private long transformId(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception e) {
            throw new IllegalArgumentException("交易Id格式异常，必须是long类型");
        }
    }

    private ReadWriteLock getUserLock(String userName) {
        return lockMap.computeIfAbsent(userName, k -> new ReentrantReadWriteLock());
    }

    private void checkDuplicateTransaction(TransactionRequest request) {
        // 获取当前毫秒时间戳
        long currentTime = System.currentTimeMillis();
        
        // 检查是否存在相同结构的交易
        Page<Transaction> recentTransactions = transactionRepository.findAllByUserName(
            request.getUserName(), 
            org.springframework.data.domain.PageRequest.of(0, 1)
        );

        boolean hasDuplicate = recentTransactions.getContent().stream()
            .anyMatch(transaction -> 
                transaction.getCreateTimestamp() <= currentTime &&  // 只检查当前毫秒之前的交易
                Objects.equals(transaction.getAmount(), request.getAmount()) &&
                Objects.equals(transaction.getType(), request.getType()) &&
                Objects.equals(transaction.getToUserName(), request.getToUserName()) &&
                Objects.equals(transaction.getDescription(), request.getDescription())
            );

        if (hasDuplicate) {
            throw new DuplicateTransactionException(
                String.format("检测到重复交易：用户 %s 已有相同的交易记录", request.getUserName())
            );
        }
    }

    private void validateTransactionRequest(TransactionRequest request) {
        if (!StringUtils.hasText(request.getUserName())) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        if (request.getUserName().length() > 20) {
            throw new IllegalArgumentException("用户名长度不能超过20个字符");
        }

        if (request.getType() == Transaction.TransactionType.TRANSFER) {
            if (!StringUtils.hasText(request.getToUserName())) {
                throw new IllegalArgumentException("转账接收方用户名不能为空");
            }
            if (request.getToUserName().length() > 20) {
                throw new IllegalArgumentException("转账接收方用户名长度不能超过20个字符");
            }
            if (request.getUserName().equals(request.getToUserName())) {
                throw new IllegalArgumentException("不能转账给自己");
            }
        }

        if (request.getAmount() == null) {
            throw new IllegalArgumentException("交易金额不能为空");
        }

        if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("交易金额必须大于0");
        }

        if (request.getAmount().scale() > 2) {
            throw new IllegalArgumentException("交易金额小数位不能超过2位");
        }

        if (request.getAmount().precision() - request.getAmount().scale() > 9) {
            throw new IllegalArgumentException("交易金额整数位不能超过9位");
        }

        if (request.getDescription() != null && request.getDescription().length() > 20) {
            throw new IllegalArgumentException("交易描述不能超过20个字符");
        }
    }

    @Override
    @CacheEvict(value = "transactions", key = "#userName + '-' + #id")
    public Transaction updateTransaction(String userName, String id, TransactionRequest request) {
        if (!userName.equals(request.getUserName())) {
            throw new IllegalArgumentException("不允许更新交易用户名");
        }
        validateTransactionRequest(request);
        
        Transaction existingTransaction = getTransaction(userName, id);

        Transaction updatedTransaction = Transaction.builder()
                .id(transformId(id))
                .userName(userName)
                .toUserName(request.getToUserName())
                .amount(request.getAmount())
                .type(request.getType())
                .description(request.getDescription())
                .createTimestamp(existingTransaction.getCreateTimestamp())
                .updateTimestamp(System.currentTimeMillis())
                .build();


        ReadWriteLock lock = getUserLock(request.getUserName());
        lock.writeLock().lock();
        try {
            return transactionRepository.save(updatedTransaction);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    @CacheEvict(value = "transactions", key = "#userName + '-' + #id")
    public void deleteTransaction(String userName, String id) {
        ReadWriteLock lock = getUserLock(userName);
        lock.writeLock().lock();
        try {
            transactionRepository.deleteByUserNameAndId(userName, transformId(id))
                    .orElseThrow(() -> new TransactionNotFoundException(
                    String.format("未找到用户 %s 的交易记录: %s", userName, id)));;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    @Cacheable(value = "transactions", key = "#userName + '-' + #id")
    public Transaction getTransaction(String userName, String id) {
        if (!StringUtils.hasText(userName)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        // 这里应该不需要加锁，目前不考虑事务性
        return transactionRepository.findByUserNameAndId(userName, transformId(id))
                .orElseThrow(() -> new TransactionNotFoundException(
                    String.format("未找到用户 %s 的交易记录: %s", userName, id)));
    }

    @Override
    @Cacheable(value = "transactions", key = "#userName + '-all-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Transaction> getAllTransactions(String userName, Pageable pageable) {
        if (!StringUtils.hasText(userName)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        // 限制分页大小最大为100
        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("每页大小不能超过100条记录");
        }

        ReadWriteLock lock = getUserLock(userName);
        lock.readLock().lock();
        try {
            return transactionRepository.findAllByUserName(userName, pageable);
        } finally {
            lock.readLock().unlock();
        }
    }
} 