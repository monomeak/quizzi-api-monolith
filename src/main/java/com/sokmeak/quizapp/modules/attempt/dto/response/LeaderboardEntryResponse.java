package com.sokmeak.quizapp.modules.attempt.dto.response;

import java.time.LocalDateTime;

public record LeaderboardEntryResponse(
        int rank,
        String username,
        String displayName,
        int correctCount,
        int totalQuestions,
        double scorePercent,
        LocalDateTime submittedAt)
 {
}
