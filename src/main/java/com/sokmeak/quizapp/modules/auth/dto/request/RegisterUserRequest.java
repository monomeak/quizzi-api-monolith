package com.sokmeak.quizapp.modules.auth.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "username is required")
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "username may contain letters, digits, . _ - only")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "email is not valid")
        String email,

        @NotBlank(message = "displayName is required")
        @Size(max = 100)
        String displayName,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8-72 characters")
        String password
) {
}
