package com.sokmeak.quizapp.modules.attempt.impl;

import com.sokmeak.quizapp.common.exception.BadRequestException;
import com.sokmeak.quizapp.common.exception.ConflictException;
import com.sokmeak.quizapp.common.exception.NotFoundException;
import com.sokmeak.quizapp.modules.attempt.dto.request.JoinQuizRequest;
import com.sokmeak.quizapp.modules.attempt.dto.request.SubmitAttemptRequest;
import com.sokmeak.quizapp.modules.attempt.dto.response.AttemptHistoryResponse;
import com.sokmeak.quizapp.modules.attempt.dto.response.AttemptResponse;
import com.sokmeak.quizapp.modules.attempt.dto.response.AttemptResultResponse;
import com.sokmeak.quizapp.modules.attempt.dto.response.LeaderboardEntryResponse;
import com.sokmeak.quizapp.modules.attempt.entities.QuizAttempt;
import com.sokmeak.quizapp.modules.attempt.mapper.AttemptMapper;
import com.sokmeak.quizapp.modules.attempt.repository.AttemptRepository;
import com.sokmeak.quizapp.modules.attempt.service.AttemptService;
import com.sokmeak.quizapp.modules.quiz.entity.Quiz;
import com.sokmeak.quizapp.modules.quiz.repository.QuizRepository;
import com.sokmeak.quizapp.modules.user.entity.User;
import com.sokmeak.quizapp.modules.user.service.UserService;
import com.sokmeak.quizapp.utils.AttemptStatus;
import com.sokmeak.quizapp.utils.QuizStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttemptServiceImpl implements AttemptService {
    //  declare mapper
    private static final AttemptMapper MAPPER  =  AttemptMapper.INSTANCE;

    // inject some dependencies
    private final AttemptRepository attemptRepository;
    private final QuizRepository quizRepository;
    private final UserService userService;


    @Override
    @Transactional
    public AttemptResponse join(String username, JoinQuizRequest request) {

        // get the full player info
        User player = userService.requireByUsername(username);

        // get code from the request
        String code = request.joinCode().trim().toUpperCase();

        // get the quiz info
        Quiz quiz = quizRepository.findByJoinCode(code).orElseThrow(()-> new NotFoundException("Quiz code not found: " + code));

        // confirm the status of the quiz
        if(quiz.getStatus()!= QuizStatus.PUBLISHED){
            throw new BadRequestException("Quiz '" + quiz.getTitle() + "' is not open for joining");
        }

        // check if the owner of the quiz is attempt to join
        if(quiz.getOwner().getId().equals(player.getId())){
            throw new BadRequestException("You cannot join a quiz you created");

        }

        // One attempt per person per quiz. Re-joining an unfinished attempt is fine
        // (the phone died, the tab was closed); re-joining a finished one is a 409.

        var existing = attemptRepository.findByQuizIdAndUserId(quiz.getId(), player.getId());
        if(existing.isPresent()){
            QuizAttempt attempt = existing.get();
            if(attempt.getStatus() == AttemptStatus.SUBMITTED){
                throw new ConflictException("You have already completed this quiz");

            }
            return toAttemptResponse(attempt,quiz);

        }
        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .user(player)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();

       QuizAttempt saved = attemptRepository.save(attempt);

        log.info("User {} joined quiz id={} as attempt id={}", username, quiz.getId(), saved.getId());
        return toAttemptResponse(saved,quiz);

    }

    @Override
    public AttemptResponse findMyAttempt(Long attemptId, String username) {
        return null;
    }

    @Override
    public AttemptResultResponse submit(Long attemptId, String username, SubmitAttemptRequest request) {
        return null;
    }

    @Override
    public List<AttemptHistoryResponse> myHistory(String username) {
        return List.of();
    }

    @Override
    public List<LeaderboardEntryResponse> leaderboard(Long quizId) {
        return List.of();
    }

    private AttemptResponse toAttemptResponse(QuizAttempt attempt, Quiz quiz) {
        return new AttemptResponse(
                attempt.getId(),
                quiz.getId(),
                quiz.getTitle(),
                attempt.getStatus().name(),
                attempt.getStartedAt(),
                MAPPER.questionsToAttemptQuestions(quiz.getQuestions()));
    }

}
