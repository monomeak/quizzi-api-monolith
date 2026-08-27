package com.sokmeak.quizapp.modules.attempt.dto.response;

import java.time.LocalDateTime;

public record AttemptHistoryResponse(
        Long attemptId,
        Long quizId,
        String quizTitle,
        String category,
        String status,
        Integer correctCount,
        Integer totalQuestions,
        Double scorePercent,
        LocalDateTime startedAt,
        LocalDateTime submittedAt
) {
}
