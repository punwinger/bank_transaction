package com.example.transaction.repository.impl;

import com.example.transaction.exception.PageOutOfRangeException;
import com.example.transaction.exception.TransactionTooManyException;
import com.example.transaction.exception.UserTooManyException;
import com.example.transaction.model.Transaction;
import com.example.transaction.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {
    // 主存储：userName -> (id -> Transaction)
    private final Map<String, ConcurrentSkipListMap<Long, Transaction>> store = new ConcurrentHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        if (store.size() >= 100000000 && !store.containsKey(transaction.getUserName())) {
            throw new UserTooManyException("交易用户超过上限100000000");
        }

        // 获取用户的交易存储
        Map<Long, Transaction> userTransactions = store.computeIfAbsent(
            transaction.getUserName(), 
            k -> new ConcurrentSkipListMap<>()
        );

        if (userTransactions.size() >= 100000000) {
            throw new TransactionTooManyException("单个用户的交易超过上限100000000");
        }

        // 保存交易
        userTransactions.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public Optional<Transaction> findByUserNameAndId(String userName, long id) {
        Map<Long, Transaction> userTransactions = store.get(userName);
        if (userTransactions == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userTransactions.get(id));
    }

    @Override
    public Optional<Transaction> deleteByUserNameAndId(String userName, long id) {
        Map<Long, Transaction> userTransactions = store.get(userName);
        if (userTransactions != null) {
            return Optional.ofNullable(userTransactions.remove(id));
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
} 