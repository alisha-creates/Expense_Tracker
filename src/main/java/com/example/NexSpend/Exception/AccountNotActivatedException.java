package com.example.NexSpend.Exception;

public class AccountNotActivatedException extends RuntimeException{
    public AccountNotActivatedException(String message) {
        super(message);
    }
}
