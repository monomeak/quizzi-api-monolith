package com.sokmeak.quizapp.modules.attempt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public  record JoinQuizRequest(
        @NotBlank(message = "Join Code is required")
        @Size(min = 4, max = 10, message = "Join Code must be between 4 and 10 characters")
        String joinCode
) {
}
