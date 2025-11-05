package ru.netology.cloud_api.exception;

public class Unauthorized401Exception extends RuntimeException {
    public Unauthorized401Exception(String message) { super(message); }
}

