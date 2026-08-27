package com.sokmeak.quizapp.modules.quiz.dto.response;

import java.time.LocalDateTime;

public record QuizSummaryResponse (
        Long id,
        String title,
        String description,
        String category,
        String status,
        String joinCode,
        String ownerUsername,
        int questionCount,
        LocalDateTime createdAt
){
}
