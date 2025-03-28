package com.example.transaction.exception;

public class UserTooManyException extends RuntimeException {
    public UserTooManyException(String message) {
        super(message);
    }
} 