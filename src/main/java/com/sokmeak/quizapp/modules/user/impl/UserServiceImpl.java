package com.sokmeak.quizapp.modules.user.impl;

import com.sokmeak.quizapp.common.exception.ConflictException;
import com.sokmeak.quizapp.common.exception.NotFoundException;
import com.sokmeak.quizapp.modules.user.Role;
import com.sokmeak.quizapp.modules.auth.dto.request.RegisterUserRequest;
import com.sokmeak.quizapp.modules.user.dto.UserResponse;
import com.sokmeak.quizapp.modules.user.entity.User;
import com.sokmeak.quizapp.modules.user.mapper.UserMapper;
import com.sokmeak.quizapp.modules.user.repository.UserRepository;
import com.sokmeak.quizapp.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class UserServiceImpl implements UserService {

    private static  final UserMapper MAPPER = Mappers.getMapper(UserMapper.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        var username = request.username();
        var email = request.email();

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username '" + username + "' is already taken");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email '" + email + "' is already registered");
        }
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
        User savedUser = userRepository.save(user);
        log.info("Registered user id={} username={}", savedUser.getId(), savedUser.getUsername());
        return MAPPER.userToUserResponse(savedUser);
    }

    @Override
    public UserResponse findByUsername(String username) {
        return MAPPER.userToUserResponse(requireByUsername(username));
    }

    @Override
    public User requireByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '" + username + "' was not found"));
    }
}
