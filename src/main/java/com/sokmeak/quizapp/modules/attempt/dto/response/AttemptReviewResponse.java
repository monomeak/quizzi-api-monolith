package com.sokmeak.quizapp.modules.attempt.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** The full "how did I do?" screen for one finished attempt. */
public record AttemptReviewResponse(
        Long attemptId,
        Long quizId,
        String quizTitle,
        String category,
        String status,
        int correctCount,
        int incorrectCount,
        int skippedCount,
        int totalQuestions,
        double scorePercent,
        boolean passed,
        /** One-line verdict on the whole attempt, e.g. "Great job - 8 out of 10." */
        String summary,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        /** How long the run took, null if the attempt has no submission time. */
        Long durationSeconds,
        List<AnswerReviewResponse> answers
) {
}
