package com.sokmeak.quizapp.modules.quiz.service;

import com.sokmeak.quizapp.modules.quiz.dto.request.CreateQuizRequest;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizDetailResponse;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizSummaryResponse;

import java.util.List;

public interface QuizService {
    QuizDetailResponse createQuiz(String username,CreateQuizRequest createQuizRequest);

    List<QuizSummaryResponse> findMine(String username);

    List<QuizSummaryResponse> findPublished();

    // Owner only include answers and correct answers

    QuizDetailResponse findDetailForOwner(Long quizId, String username);

    QuizSummaryResponse publish(Long quizId, String username);

    QuizSummaryResponse close(Long quizId, String username);

}
