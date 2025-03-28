package com.example.transaction.exception;

public class TransactionTooManyException extends RuntimeException {
    public TransactionTooManyException(String message) {
        super(message);
    }
} 