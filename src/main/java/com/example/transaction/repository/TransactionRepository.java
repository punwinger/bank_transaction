package com.example.transaction.repository;

import com.example.transaction.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findByUserNameAndId(String userName, long id);
    Optional<Transaction> deleteByUserNameAndId(String userName, long id);
    Page<Transaction> findAllByUserName(String userName, Pageable pageable);
    Optional<Transaction> findLastByUserName(String userName);
} 