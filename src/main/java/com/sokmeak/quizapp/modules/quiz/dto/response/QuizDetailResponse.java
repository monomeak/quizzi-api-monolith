package com.sokmeak.quizapp.modules.quiz.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record QuizDetailResponse (
        Long id,
        String title,
        String description,
        String category,
        String status,
        String joinCode,
        String ownerUsername,
        LocalDateTime createdAt,
        List<QuestionResponse> questions
){
}
