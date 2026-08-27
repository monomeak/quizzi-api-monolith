package com.sokmeak.quizapp.modules.quiz.controller;


import com.sokmeak.quizapp.modules.quiz.dto.request.CreateQuizRequest;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizDetailResponse;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizSummaryResponse;
import com.sokmeak.quizapp.modules.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
@Tag(name = "Quizzes", description = "Create quizzes and publish them for others to join")
public class QuizController {
    private final QuizService quizService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a quiz with its questions (starts as DRAFT)")
    public QuizDetailResponse create(Authentication authentication,
                                     @Valid @RequestBody CreateQuizRequest request) {
        return quizService.createQuiz(authentication.getName(), request);
    }



    @GetMapping("/mine")
    @Operation(summary = "Quizzes I created")
    public List<QuizSummaryResponse> mine(Authentication authentication) {
        return quizService.findMine(authentication.getName());
    }

    @GetMapping("/published")
    @Operation(summary = "Quizzes open to join")
    public List<QuizSummaryResponse> published() {
        return quizService.findPublished();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full quiz including answers - owner only")
    public QuizDetailResponse detail(@PathVariable Long id, Authentication authentication) {
        return quizService.findDetailForOwner(id, authentication.getName());
    }


    @PostMapping("/{id}/publish")
    @Operation(summary = "Open the quiz and get its join code")
    public QuizSummaryResponse publish(@PathVariable Long id, Authentication authentication) {
        return quizService.publish(id, authentication.getName());
    }



    @PostMapping("/{id}/close")
    @Operation(summary = "Stop accepting new attempts")
    public QuizSummaryResponse close(@PathVariable Long id, Authentication authentication) {
        return quizService.close(id, authentication.getName());
    }




}
