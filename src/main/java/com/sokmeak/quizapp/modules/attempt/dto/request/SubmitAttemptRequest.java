package com.sokmeak.quizapp.modules.attempt.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public record SubmitAttemptRequest(
        @NotEmpty(message = "answers must not be empty")
        @Valid
        List<AnswerSubmission> answers
) {
}
