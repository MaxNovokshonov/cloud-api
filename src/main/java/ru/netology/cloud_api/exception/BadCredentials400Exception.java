package ru.netology.cloud_api.exception;

public class BadCredentials400Exception extends RuntimeException {
    public BadCredentials400Exception(String message) {
        super(message);
    }
}
