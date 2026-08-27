package com.sokmeak.quizapp.modules.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateQuizRequest(
        @NotBlank(message = "title is required")
        @Size(max = 150)
        String title,

        @Size(max = 500)
        String description,

        @Size(max = 50)
        String category,

        @NotEmpty(message = "a quiz needs at least one question")
        @Size(max = 50, message = "50 questions is the maximum")
        @Valid
        List<CreateQuestionRequest> questions
) {
}
