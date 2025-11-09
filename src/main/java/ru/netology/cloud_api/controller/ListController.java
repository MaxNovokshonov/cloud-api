package ru.netology.cloud_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.netology.cloud_api.dto.FileListItem;
import ru.netology.cloud_api.service.FileService;

import java.util.List;
import java.util.UUID;

@RestController
public class ListController {

    private final FileService fileService;

    public ListController(FileService fileService) {
        this.fileService = fileService;
    }

    private static UUID currentUserId() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        Object p = a != null ? a.getPrincipal() : null;
        if (p instanceof UUID u) return u;
        return UUID.fromString(String.valueOf(p));
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileListItem>> list(@RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(fileService.list(currentUserId(), limit));
    }
}
