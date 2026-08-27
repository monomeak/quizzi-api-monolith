package com.sokmeak.quizapp.modules.attempt.dto.response;

public record AttemptQuestionResponse(
        Long questionId,
        int position,
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD
) {
}
