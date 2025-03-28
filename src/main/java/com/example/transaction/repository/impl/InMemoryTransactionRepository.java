package com.example.transaction.repository.impl;

import com.example.transaction.exception.PageOutOfRangeException;
import com.example.transaction.exception.TransactionTooManyException;
import com.example.transaction.model.Transaction;
import com.example.transaction.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {
    // 主存储：userName -> (id -> Transaction)
    private final Map<String, ConcurrentSkipListMap<Long, Transaction>> store = new ConcurrentHashMap<>();

    // 二级索引：id -> Transaction
    private final Map<Long, Transaction> idIndex = new ConcurrentHashMap<>();

    private int maxSize = 20000000;

    public void setMaxSize(int size) {
        this.maxSize = size;
    }

    @Override
    public Transaction save(Transaction transaction) {
        if (idIndex.size() > maxSize) {
            throw new TransactionTooManyException("交易总数不能超过最大值:" + maxSize);
        }

        // 获取用户的交易存储
        Map<Long, Transaction> userTransactions = store.computeIfAbsent(
            transaction.getUserName(), 
            k -> new ConcurrentSkipListMap<>()
        );

        // 保存交易
        userTransactions.put(transaction.getId(), transaction);

        idIndex.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public Optional<Transaction> findByUserNameAndId(String userName, long id) {
        Transaction transaction = idIndex.get(id);
        if (transaction == null || !transaction.getUserName().equals(userName)) {
            return Optional.empty();
        }
        return Optional.of(transaction);
    }

    @Override
    //non thread-safe
    public Optional<Transaction> deleteByUserNameAndId(String userName, long id) {
        Map<Long, Transaction> userTransactions = store.get(userName);
        if (userTransactions != null) {
            Transaction oldTransaction = userTransactions.remove(id);

            if (oldTransaction != null) {
                idIndex.remove(id);
                if (userTransactions.isEmpty()) {
                    store.remove(userName);
                }
            }
            return Optional.ofNullable(oldTransaction);

        }
        return Optional.empty();
    }

    @Override
    //Non-threadsafe
    public Page<Transaction> findAllByUserName(String userName, Pageable pageable) {
        ConcurrentSkipListMap<Long, Transaction> userTransactions = store.get(userName);
        if (userTransactions == null || userTransactions.isEmpty()) {
            return Page.empty(pageable);
        }

        long offset = pageable.getOffset();
        int totalSize = userTransactions.size();
        if (offset >= totalSize) {
            throw new PageOutOfRangeException("page参数超过范围，总数:" + userTransactions.size());
        }

        List<Transaction> pageContent = new ArrayList<>(pageable.getPageSize());
        Long startKey = userTransactions.keySet().stream()
                .skip(offset)
                .findFirst()
                .orElse(null);

        if (startKey == null) {
            throw new PageOutOfRangeException("page参数超过范围，总数:" + userTransactions.size());
        }

        // 获取从 startKey 开始的 size 条记录
        userTransactions.tailMap(startKey).values().stream()
                .limit(pageable.getPageSize())
                .forEachOrdered(pageContent::add);

        return new PageImpl<>(
            pageContent,
            pageable, totalSize
        );
    }

    @Override
    public Optional<Transaction> findLastByUserName(String userName) {
        ConcurrentSkipListMap<Long, Transaction> userTransactions = store.get(userName);
        if (userTransactions == null || userTransactions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(userTransactions.lastEntry().getValue());
    }
} 