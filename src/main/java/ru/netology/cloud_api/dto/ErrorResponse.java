package ru.netology.cloud_api.dto;

public class ErrorResponse {
    private String message;
    private Integer id;

    public ErrorResponse() {
    }

    public ErrorResponse(Integer id, String message) {
        this.id = id;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Integer getId() {
        return id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
