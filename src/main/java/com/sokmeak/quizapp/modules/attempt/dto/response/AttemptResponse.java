package com.sokmeak.quizapp.modules.attempt.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AttemptResponse(
        Long attemptId,
        Long quizId,
        String quizTitle,
        String status,
        LocalDateTime startedAt,
        List<AttemptQuestionResponse> questions
) {
}
