package com.example.transaction.service;

import com.example.transaction.dto.TransactionRequest;
import com.example.transaction.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {
    Transaction createTransaction(TransactionRequest request);
    Transaction updateTransaction(String userName, String id, TransactionRequest request);
    void deleteTransaction(String userName, String id);
    Transaction getTransaction(String userName, String id);
    Page<Transaction> getAllTransactions(String userName, Pageable pageable);
} 