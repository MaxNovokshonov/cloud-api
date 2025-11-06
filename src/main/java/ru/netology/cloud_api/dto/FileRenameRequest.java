package ru.netology.cloud_api.dto;

import jakarta.validation.constraints.NotBlank;

public class FileRenameRequest {
    @NotBlank
    private String name;

    public FileRenameRequest() {
    }

    public FileRenameRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
