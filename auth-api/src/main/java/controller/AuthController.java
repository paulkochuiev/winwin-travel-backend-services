package com.winwin.travel.authapi.controller;

import com.winwin.travel.authapi.dto.RegisterRequest;
import com.winwin.travel.authapi.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {

        authService.register(request.getEmail(), request.getPassword());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
