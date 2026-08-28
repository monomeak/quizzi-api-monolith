package com.sokmeak.quizapp.modules.attempt.controllers;


import com.sokmeak.quizapp.modules.attempt.dto.request.JoinQuizRequest;
import com.sokmeak.quizapp.modules.attempt.dto.request.SubmitAttemptRequest;
import com.sokmeak.quizapp.modules.attempt.dto.response.AttemptHistoryResponse;
import com.sokmeak.quizapp.modules.attempt.dto.response.AttemptResponse;
import com.sokmeak.quizapp.modules.attempt.dto.response.AttemptResultResponse;
import com.sokmeak.quizapp.modules.attempt.service.AttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attempts")
@RequiredArgsConstructor
@Tag(name = "Attempts", description = "Join a quiz, answer it, and see your history")
public class AttemptController {

    private final AttemptService attemptService;



    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Join a published quiz with its code; returns the questions without answers")
    public AttemptResponse join(Authentication authentication, @Valid @RequestBody JoinQuizRequest request) {
        return attemptService.join(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Re-open an attempt you already started")
    public AttemptResponse reOpen(Authentication authentication, @PathVariable Long id) {
        return attemptService.findMyAttempt(id, authentication.getName());
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit an attempt and get the score")
    public AttemptResultResponse submit(Authentication authentication, @PathVariable Long id, @Valid @RequestBody SubmitAttemptRequest request) {
        return attemptService.submit(id, authentication.getName(),request);
    }


    @GetMapping("/me")
    @Operation(summary = "Quizzes I have taken")
    public List<AttemptHistoryResponse> myHistory(Authentication authentication) {
        return attemptService.myHistory(authentication.getName());
    }

}
