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
import com.sokmeak.quizapp.utils.AnswerVerdict;
import com.sokmeak.quizapp.utils.AttemptStatus;
import com.sokmeak.quizapp.utils.OptionKey;
import com.sokmeak.quizapp.utils.QuizStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    /** Score at or above this counts as a pass on the review screen. */
    private static final double PASS_MARK = 50d;

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

            // A skipped question (null) is simply wrong: it can never match a correct option.
            boolean correct = selectedOption != null && selectedOption == question.getCorrectOption();
            if(correct) {
                correctCount++;
            }
            // Store the answer so the result stays reproducible later

            attempt.addAnswer(AttemptAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedOption(selectedOption)
                    .correct(correct).build());

            feedbackResponses.add(new AnswerFeedbackResponse(
                    question.getId(),
                    question.getQuestionText(),
                    selectedOption == null ? null:selectedOption.name(),
                    question.getCorrectOption().name(),
                    correct

            ));

        }

        // Scored once, after every question has been marked - not per iteration.
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

    @Override
    public List<AttemptHistoryResponse> myHistory(String username) {
        User player =  userService.requireByUsername(username);
        List<QuizAttempt> responses = new ArrayList<>();
        responses = attemptRepository.findAllByUserIdOrderByStartedAtDesc(player.getId());
        return MAPPER.attemptsToHistory(responses);
    }


    @Override
    public AttemptReviewResponse review(Long attemptId, String username) {

        // One query brings back the attempt, its quiz and every answer with its question.
        QuizAttempt attempt = attemptRepository.findByIdWithAnswers(attemptId)
                .orElseThrow(() -> new NotFoundException("Attempt not found: " + attemptId));

        requireOwner(attempt, username);

        // Answers are only written at submit time, and the correct options must stay
        // hidden while the quiz is still being played - so an unfinished run has
        // nothing to review yet.
        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new ConflictException("Submit this attempt before reviewing it");
        }

        Quiz quiz = attempt.getQuiz();

        List<AnswerReviewResponse> reviewed = attempt.getAnswers().stream()
                .sorted(Comparator.comparing((AttemptAnswer answer) -> answer.getQuestion().getPosition()))
                .map(this::toAnswerReview)
                .toList();

        int correctCount = 0;
        int skippedCount = 0;
        for (AttemptAnswer answer : attempt.getAnswers()) {
            if (answer.isCorrect()) {
                correctCount++;
            } else if (answer.getSelectedOption() == null) {
                // Blank answers are still wrong, but the player is told they left it empty.
                skippedCount++;
            }
        }

        int total = reviewed.size();
        int incorrectCount = total - correctCount - skippedCount;

        // The stored score is what the leaderboard shows, so the review must repeat it
        // rather than compute a second, possibly different, number.
        double percent = attempt.getScorePercent() != null
                ? attempt.getScorePercent()
                : (total == 0 ? 0d : round2((correctCount * 100d) / total));

        Long durationSeconds = attempt.getSubmittedAt() == null
                ? null
                : Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).toSeconds();

        return new AttemptReviewResponse(
                attempt.getId(),
                quiz.getId(),
                quiz.getTitle(),
                quiz.getCategory(),
                attempt.getStatus().name(),
                correctCount,
                incorrectCount,
                skippedCount,
                total,
                percent,
                percent >= PASS_MARK,
                summaryFor(percent, correctCount, total),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                durationSeconds,
                reviewed);
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
        requireOwner(attempt, username);
        return attempt;
    }

    private void requireOwner(QuizAttempt attempt, String username) {
        if (!attempt.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("You are not the owner of this attempt");
        }
    }

    private AnswerReviewResponse toAnswerReview(AttemptAnswer answer) {
        Question question = answer.getQuestion();
        OptionKey selected = answer.getSelectedOption();
        OptionKey correctKey = question.getCorrectOption();

        AnswerVerdict verdict = answer.isCorrect()
                ? AnswerVerdict.CORRECT
                : selected == null ? AnswerVerdict.SKIPPED : AnswerVerdict.INCORRECT;

        return new AnswerReviewResponse(
                question.getId(),
                question.getPosition(),
                question.getQuestionText(),
                question.getOptionA(),
                question.getOptionB(),
                question.getOptionC(),
                question.getOptionD(),
                selected == null ? null : selected.name(),
                optionText(question, selected),
                correctKey.name(),
                optionText(question, correctKey),
                answer.isCorrect(),
                verdict.name(),
                feedbackFor(verdict, question, correctKey));
    }

    /**
     * The wording rule for the review screen, kept in one place: praise when the
     * answer was right, otherwise spell out the answer that would have been.
     */
    private String feedbackFor(AnswerVerdict verdict, Question question, OptionKey correctKey) {
        String rightAnswer = correctKey.name() + ". " + optionText(question, correctKey);
        return switch (verdict) {
            case CORRECT -> "Well done! That is the right answer.";
            case SKIPPED -> "You skipped this one. The correct answer was " + rightAnswer;
            case INCORRECT -> "Not quite. The correct answer was " + rightAnswer;
        };
    }

    /** The text behind a letter, so the client never has to map A..D itself. */
    private String optionText(Question question, OptionKey key) {
        if (key == null) {
            return null;
        }
        return switch (key) {
            case A -> question.getOptionA();
            case B -> question.getOptionB();
            case C -> question.getOptionC();
            case D -> question.getOptionD();
        };
    }

    private String summaryFor(double percent, int correctCount, int total) {
        if (total == 0) {
            return "This quiz had no questions to answer.";
        }
        String score = correctCount + " out of " + total + " correct";
        if (percent >= 100d) {
            return "Perfect score - " + score + "!";
        }
        if (percent >= 80d) {
            return "Great job - " + score + ".";
        }
        if (percent >= PASS_MARK) {
            return "You passed - " + score + ".";
        }
        if (percent >= 30d) {
            return "Almost there - " + score + ". Look over the ones you missed and try the next quiz.";
        }
        return "Tough round - " + score + ". The correct answers are below.";
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
