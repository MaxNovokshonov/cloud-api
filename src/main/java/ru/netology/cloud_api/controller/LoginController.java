package ru.netology.cloud_api.controller;

import ru.netology.cloud_api.dto.LoginRequest;
import ru.netology.cloud_api.dto.LoginResponse;
import ru.netology.cloud_api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req.getLogin(), req.getPassword());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
