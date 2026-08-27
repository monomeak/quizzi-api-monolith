package com.sokmeak.quizapp.modules.attempt.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AnswerSubmission(
        @NotNull(message = "questionId is required")
        Long questionId,

        /** null or omitted counts as "skipped". */
        @Pattern(regexp = "(?i)^[ABCD]", message = "selectedOption must be A, B, C or D")
        String selectedOption
) {
}
