package ru.netology.cloud_api.exception;

public class BadRequest400Exception extends RuntimeException {
    public BadRequest400Exception(String message) {
        super(message);
    }
}
