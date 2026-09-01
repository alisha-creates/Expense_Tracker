package com.example.NexSpend.Exception;

public class RefreshTokenExpiredException extends RuntimeException{
    public RefreshTokenExpiredException(String message) {
        super(message);
    }
}
