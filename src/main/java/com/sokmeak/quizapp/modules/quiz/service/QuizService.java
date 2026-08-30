package com.sokmeak.quizapp.modules.quiz.service;

import java.util.List;

import com.sokmeak.quizapp.modules.quiz.dto.request.CreateQuizRequest;
import com.sokmeak.quizapp.modules.quiz.dto.request.UpdateQuizRequest;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizDetailResponse;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizSummaryResponse;

public interface QuizService {
    QuizDetailResponse createQuiz(String username,CreateQuizRequest createQuizRequest);

    /** Owner only. Questions can only be swapped out while the quiz is a DRAFT. */
    QuizDetailResponse update(Long quizId, String username, UpdateQuizRequest updateQuizRequest);

    /** Owner only, and only while nobody has played it - otherwise close it instead. */
    void delete(Long quizId, String username);

    

    List<QuizSummaryResponse> findMine(String username);

    List<QuizSummaryResponse> findPublished();

    // Owner only include answers and correct answers

    QuizDetailResponse findDetailForOwner(Long quizId, String username);

    QuizSummaryResponse publish(Long quizId, String username);

    QuizSummaryResponse close(Long quizId, String username);

}
