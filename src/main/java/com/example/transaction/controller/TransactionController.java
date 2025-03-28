package com.example.transaction.controller;

import com.example.transaction.dto.TransactionRequest;
import com.example.transaction.model.Transaction;
import com.example.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userName}/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(
            @PathVariable String userName,
            @Valid @RequestBody TransactionRequest request) {
        request.setUserName(userName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> updateTransaction(
            @PathVariable String userName,
            @PathVariable String id,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.updateTransaction(userName, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable String userName,
            @PathVariable String id) {
        transactionService.deleteTransaction(userName, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(
            @PathVariable String userName,
            @PathVariable String id) {
        return ResponseEntity.ok(transactionService.getTransaction(userName, id));
    }

    @GetMapping
    public ResponseEntity<Page<Transaction>> getAllTransactions(
            @PathVariable String userName,
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.getAllTransactions(userName, pageable));
    }
} 