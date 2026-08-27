package com.sokmeak.quizapp.modules.attempt.dto.response;

public record AnswerFeedbackResponse(
        Long questionId,
        String questionText,
        String selectedOption,
        String correctOption,
        boolean correct
) {
}
