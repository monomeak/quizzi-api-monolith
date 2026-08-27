package com.sokmeak.quizapp.modules.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateQuestionRequest(
        // at least two options are provided.

        @NotBlank(message = "questionText is required")
        @Size(max = 500)
        String questionText,

        @NotBlank(message = "optionA is required")
        String optionA,

        @NotBlank(message = "optionB is required")
        String optionB,

        String optionC,
        String optionD,

        @NotBlank(message = "correctOption is required")
        @Pattern(regexp = "(?i)[ABCD]", message = "correctOption must be A, B, C or D")
        String correctOption,

        /** EASY, MEDIUM or HARD. Optional; the mapper accepts any casing. */
        String difficulty
) {
}
