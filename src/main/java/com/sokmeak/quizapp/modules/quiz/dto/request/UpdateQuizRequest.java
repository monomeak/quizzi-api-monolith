package com.sokmeak.quizapp.modules.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A full replacement of the editable parts of a quiz (PUT, not PATCH): every
 * field sent here overwrites what was stored. The join code, status and owner
 * are not editable - they are managed by publish/close and by whoever created it.
 */
public record UpdateQuizRequest(
        @NotBlank(message = "title is required")
        @Size(max = 150)
        String title,

        @Size(max = 500)
        String description,

        @Size(max = 50)
        String category,

        /**
         * Optional. Leave it out to rename a quiz without touching its questions;
         * send it to REPLACE the whole list - which is only allowed while the quiz
         * is still a DRAFT.
         */
        @Size(min = 1, max = 50, message = "a quiz needs between 1 and 50 questions")
        @Valid
        List<CreateQuestionRequest> questions
) {
}
