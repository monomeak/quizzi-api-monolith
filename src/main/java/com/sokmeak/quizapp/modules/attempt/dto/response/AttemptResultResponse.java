package com.sokmeak.quizapp.modules.attempt.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AttemptResultResponse(
        Long attemptId,
        Long quizId,
        String quizTitle,
        int correctCount,
        int totalQuestions,
        double scorePercent,
        LocalDateTime submittedAt,
        List<AnswerFeedbackResponse> feedback
) {
}
