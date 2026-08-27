package com.sokmeak.quizapp.modules.user.dto;

import java.time.LocalDateTime;

public record UserResponse(Long id,
                           String username,
                           String email,
                           String displayName,
                           String role,
                           LocalDateTime createdAt) {
}
