package com.sokmeak.quizapp.modules.auth.controller;


import com.sokmeak.quizapp.modules.auth.dto.request.LoginRequest;
import com.sokmeak.quizapp.modules.auth.dto.request.RegisterUserRequest;
import com.sokmeak.quizapp.modules.auth.dto.response.LoginResponse;
import com.sokmeak.quizapp.modules.auth.service.AuthService;
import com.sokmeak.quizapp.modules.user.dto.UserResponse;
import com.sokmeak.quizapp.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Exchange a username and password for a token")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    // sign in
    @PostMapping("/signin")
    @Operation(summary ="Log in and receive a JWT (public)")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // sign up
    @PostMapping("/signup")
    @Operation(summary = "Signup new account (public)")
    public UserResponse signup(@Valid @RequestBody RegisterUserRequest request) {
        return userService.register(request);
    }



}
