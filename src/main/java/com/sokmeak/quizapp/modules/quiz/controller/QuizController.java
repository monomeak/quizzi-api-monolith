package com.sokmeak.quizapp.modules.quiz.controller;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sokmeak.quizapp.modules.quiz.dto.request.CreateQuizRequest;
import com.sokmeak.quizapp.modules.quiz.dto.request.UpdateQuizRequest;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizDetailResponse;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizSummaryResponse;
import com.sokmeak.quizapp.modules.quiz.service.QuizService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
@Tag(name = "Quizzes", description = "Create quizzes and publish them for others to join")
public class QuizController {
    private final QuizService quizService;

    // CRUD QUIZ

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a quiz with its questions (starts as DRAFT)")
    public QuizDetailResponse create(Authentication authentication,
                                     @Valid @RequestBody CreateQuizRequest request) {
        return quizService.createQuiz(authentication.getName(), request);
    }



    @PutMapping("/{id}")
    @Operation(summary = "Update a quiz you own; questions can only be replaced while it is a DRAFT")
    public QuizDetailResponse update(@PathVariable Long id,
                                     Authentication authentication,
                                     @Valid @RequestBody UpdateQuizRequest request) {
        return quizService.update(id, authentication.getName(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a quiz you own; refused once somebody has played it")
    public void delete(@PathVariable Long id, Authentication authentication) {
        quizService.delete(id, authentication.getName());
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
