package com.sokmeak.quizapp.modules.attempt.impl;

import com.sokmeak.quizapp.common.exception.BadRequestException;
import com.sokmeak.quizapp.common.exception.ConflictException;
import com.sokmeak.quizapp.common.exception.NotFoundException;
import com.sokmeak.quizapp.modules.attempt.dto.request.AnswerSubmission;
import com.sokmeak.quizapp.modules.attempt.dto.request.JoinQuizRequest;
import com.sokmeak.quizapp.modules.attempt.dto.request.SubmitAttemptRequest;
import com.sokmeak.quizapp.modules.attempt.dto.response.*;
import com.sokmeak.quizapp.modules.attempt.entities.AttemptAnswer;
import com.sokmeak.quizapp.modules.attempt.entities.QuizAttempt;
import com.sokmeak.quizapp.modules.attempt.mapper.AttemptMapper;
import com.sokmeak.quizapp.modules.attempt.repository.AttemptRepository;
import com.sokmeak.quizapp.modules.attempt.service.AttemptService;
import com.sokmeak.quizapp.modules.question.entity.Question;
import com.sokmeak.quizapp.modules.quiz.entity.Quiz;
import com.sokmeak.quizapp.modules.quiz.repository.QuizRepository;
import com.sokmeak.quizapp.modules.user.entity.User;
import com.sokmeak.quizapp.modules.user.service.UserService;
import com.sokmeak.quizapp.utils.AttemptStatus;
import com.sokmeak.quizapp.utils.OptionKey;
import com.sokmeak.quizapp.utils.QuizStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

        QuizAttempt attempt = requireOwnAttempt(attemptId, username);

        return toAttemptResponse(attempt, attempt.getQuiz());
    }

    @Override
    @Transactional
    public AttemptResultResponse submit(Long attemptId, String username, SubmitAttemptRequest request) {

        QuizAttempt attempt = requireOwnAttempt(attemptId, username);
        // check the attempt status
        if(attempt.getStatus() == AttemptStatus.SUBMITTED){
            throw new ConflictException("You have already completed this quiz");
        }

        // get the actual quiz
        Quiz quiz = attempt.getQuiz();

        // processing skipped as needed
        // questionId -> chosen letter, so a missing entry simply means "skipped"

        Map<Long, OptionKey> chosenOptions = new HashMap<>();

        for (AnswerSubmission submission: request.answers()) {
            chosenOptions.put(submission.questionId(), parseOption(submission.selectedOption()));
        }

        List<AnswerFeedbackResponse> feedbackResponses = new ArrayList<>();

        int correctCount = 0;

        for(Question question: quiz.getQuestions()){
            // loop all question for that quiz
            OptionKey selectedOption =  chosenOptions.get(question.getId());

            // if correct
            boolean correct = selectedOption == null && selectedOption == question.getCorrectOption();
            if(correct) {
                correctCount++;
            }
            // Store the answer so the result stays reproducible later

            attempt.addAnswer(AttemptAnswer.builder().question(question)
                    .selectedOption(selectedOption)
                    .correct(correct).build());

            feedbackResponses.add(new AnswerFeedbackResponse(
                    question.getId(),
                    question.getQuestionText(),
                    selectedOption == null ? null:selectedOption.name(),
                    question.getCorrectOption().name(),
                    correct

            ));

            int total = quiz.getQuestions().size();

            double percent = total == 0 ? 0d : round2((correctCount * 100d)/total); // 7 out of 10 => 70.0 round2 still 70.00

            attempt.setStatus(AttemptStatus.SUBMITTED);
            attempt.setSubmittedAt(LocalDateTime.now());
            attempt.setCorrectCount(correctCount);
            attempt.setTotalQuestions(total);
            attempt.setScorePercent(percent);

            log.info("Attempt id={} submitted by {}: {}/{} ({}%)", attemptId, username, correctCount, total, percent);

            return new AttemptResultResponse(
                    attempt.getId(), quiz.getId(), quiz.getTitle(), correctCount, total, percent, attempt.getSubmittedAt(), feedbackResponses
            );

        }


        return null;
    }

    @Override
    public List<AttemptHistoryResponse> myHistory(String username) {
        User player =  userService.requireByUsername(username);
        List<QuizAttempt> responses = new ArrayList<>();
        responses = attemptRepository.findAllByUserIdOrderByStartedAtDesc(player.getId());
        return MAPPER.attemptsToHistory(responses);
    }

    @Override
    public List<LeaderboardEntryResponse> leaderboard(Long quizId) {

        if(!quizRepository.existsById(quizId)){
            throw new NotFoundException("Quiz id not found: " + quizId);
        }

        List<QuizAttempt> ranked = attemptRepository.findTop20ByQuizIdAndStatusOrderByScorePercentDescSubmittedAtAsc(quizId,AttemptStatus.SUBMITTED);

        List<LeaderboardEntryResponse> board = new ArrayList<>();

        int rank = 1;

        for (QuizAttempt attempt : ranked) {

            board.add(new LeaderboardEntryResponse(
                    rank++, // use first increase later
                    attempt.getUser().getUsername(), attempt.getUser().getDisplayName(),attempt.getCorrectCount(),
                    attempt.getTotalQuestions(), attempt.getScorePercent(), attempt.getSubmittedAt()));
        }

        return board;
    }


    //=== helper ===
    private QuizAttempt requireOwnAttempt(Long attemptId, String username) {
        QuizAttempt attempt = attemptRepository.findById(attemptId).orElseThrow(() -> new NotFoundException("Attempt not found: " + attemptId));
       // check if the quiz attempt is belong to the user ot not.
        if (!attempt.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("You are not the owner of this attempt");
        }
        return attempt;
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

    private OptionKey parseOption(String value){
        if(value == null || value.isBlank()){
            return null;
        }
        try{
            return OptionKey.valueOf(value.trim().toUpperCase());

        }catch (IllegalArgumentException e){
            throw new BadRequestException("selectedOption must be A, B, C or D yet was" + value);
        }
    }

    private double round2(double value){
        return Math.round(value * 100d) / 100d;
    }

}
