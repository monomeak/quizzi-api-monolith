package com.sokmeak.quizapp.modules.attempt.controllers;

import com.sokmeak.quizapp.modules.attempt.dto.response.LeaderboardEntryResponse;
import com.sokmeak.quizapp.modules.attempt.service.AttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
@Tag(name="Leaderboard", description="Leaderboard management")
public class LeadeboardController {

    private final AttemptService attemptService;

    @GetMapping("/{quizId}/leaderboard")
    @Operation(summary = "Top 20 scores for a quiz, best first")
    public List<LeaderboardEntryResponse> leaderboard(@PathVariable Long quizId) {
        return attemptService.leaderboard(quizId);
    }

}
