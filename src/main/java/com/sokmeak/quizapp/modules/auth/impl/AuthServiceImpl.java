package com.sokmeak.quizapp.modules.auth.impl;

import com.sokmeak.quizapp.modules.auth.dto.request.LoginRequest;
import com.sokmeak.quizapp.modules.auth.dto.response.LoginResponse;
import com.sokmeak.quizapp.modules.auth.jwt.JwtService;
import com.sokmeak.quizapp.modules.auth.service.AuthService;
import com.sokmeak.quizapp.modules.user.entity.User;
import com.sokmeak.quizapp.modules.user.mapper.UserMapper;
import com.sokmeak.quizapp.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.sokmeak.quizapp.constant.TokenType.BEARER;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private static final UserMapper MAPPER = UserMapper.INSTANCE;
    private final UserService userService ;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager ;
    @Override
    public LoginResponse login(LoginRequest loginRequest) {

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for username={}: {}", loginRequest.username(), e.getMessage());
            // Rethrown as-is so GlobalExceptionHandler answers 401 with one generic
            // message; wrapping it in a RuntimeException turns every bad password into a 500.
            throw e;
        }

        // authentication.getName() rather that request.username(): Spring has resolved it to the stored spelling, and it is now a proven identity

        User user = userService.requireByUsername(authentication.getName());
        log.info("Issued a token for username={}", user.getUsername());

        return new LoginResponse(
                jwtService.issue(user),
                BEARER,
                jwtService.expiresInSeconds(),
                MAPPER.userToUserResponse(user)
        );

    }
}
