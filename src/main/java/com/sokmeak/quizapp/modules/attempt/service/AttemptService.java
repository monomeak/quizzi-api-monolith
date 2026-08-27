package com.sokmeak.quizapp.modules.attempt.service;

import com.sokmeak.quizapp.modules.attempt.dto.request.JoinQuizRequest;
import com.sokmeak.quizapp.modules.attempt.dto.request.SubmitAttemptRequest;
import com.sokmeak.quizapp.modules.attempt.dto.response.AttemptHistoryResponse;
import com.sokmeak.quizapp.modules.attempt.dto.response.AttemptResponse;
import com.sokmeak.quizapp.modules.attempt.dto.response.AttemptResultResponse;
import com.sokmeak.quizapp.modules.attempt.dto.response.LeaderboardEntryResponse;

import java.util.List;

public interface AttemptService {
    AttemptResponse join(String username, JoinQuizRequest request);

    AttemptResponse findMyAttempt(Long attemptId, String username);

    AttemptResultResponse submit(Long attemptId, String username, SubmitAttemptRequest request);

    List<AttemptHistoryResponse> myHistory(String username);

    List<LeaderboardEntryResponse> leaderboard(Long quizId);
}
