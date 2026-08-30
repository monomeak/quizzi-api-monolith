package com.sokmeak.quizapp.modules.quiz.impl;

import com.sokmeak.quizapp.common.exception.ConflictException;
import com.sokmeak.quizapp.common.exception.ForbiddenException;
import com.sokmeak.quizapp.common.exception.NotFoundException;
import com.sokmeak.quizapp.modules.quiz.dto.request.CreateQuestionRequest;
import com.sokmeak.quizapp.modules.quiz.dto.request.CreateQuizRequest;
import com.sokmeak.quizapp.modules.quiz.dto.request.UpdateQuizRequest;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizDetailResponse;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizSummaryResponse;
import com.sokmeak.quizapp.modules.attempt.repository.AttemptRepository;
import com.sokmeak.quizapp.modules.quiz.entity.Quiz;
import com.sokmeak.quizapp.modules.quiz.mapper.QuizMapper;
import com.sokmeak.quizapp.modules.quiz.repository.QuizRepository;
import com.sokmeak.quizapp.modules.quiz.service.QuizService;
import com.sokmeak.quizapp.modules.user.entity.User;
import com.sokmeak.quizapp.modules.user.service.UserService;
import com.sokmeak.quizapp.utils.JoinCodeGenerator;
import com.sokmeak.quizapp.utils.QuizStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizServiceImpl implements QuizService {

    private static final QuizMapper MAPPER = Mappers.getMapper(QuizMapper.class);

    private final QuizRepository quizRepository;
    private final UserService userService;
    private final JoinCodeGenerator joinCodeGenerator;
    /** Only to count plays before a delete - the quiz module never writes attempts. */
    private final AttemptRepository attemptRepository;



    @Override
    @Transactional
    public QuizDetailResponse createQuiz(String username, CreateQuizRequest createQuizRequest) {
        User owner = userService.requireByUsername(username);


        Quiz quiz = Quiz.builder()
                .title(createQuizRequest.title())
                .description(createQuizRequest.description())
                .category(createQuizRequest.category())
                .joinCode(joinCodeGenerator.generateUnique())
                .status(QuizStatus.DRAFT)
                .owner(owner)
                .createdAt(LocalDateTime.now())
                .build();

        // addQuestion() sets the back-reference and the position, so cascade can save them all.

        for(CreateQuestionRequest createQuestionRequest : createQuizRequest.questions()) {
            quiz.addQuestion(MAPPER.createRequestToQuestion(createQuestionRequest));
        }

        Quiz savedQuiz = quizRepository.save(quiz);

        log.info("User {} created quiz id={} with {} questions",
                username, savedQuiz.getId(), savedQuiz.getQuestions().size());

        return MAPPER.quizToDetail(savedQuiz);
    }


    @Override
    @Transactional
    public QuizDetailResponse update(Long quizId, String username, UpdateQuizRequest updateQuizRequest) {
        // With questions, because replacing them means removing the old rows.
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId).orElseThrow(() -> NotFoundException.of("Quiz", quizId));
        requireOwner(quiz, username);

        // PUT semantics: whatever is sent wins, and an omitted description clears it.
        quiz.setTitle(updateQuizRequest.title());
        quiz.setDescription(updateQuizRequest.description());
        quiz.setCategory(updateQuizRequest.category());

        List<CreateQuestionRequest> newQuestions = updateQuizRequest.questions();
        if (newQuestions != null) {
            // Rewriting the questions of a live quiz would rewrite history: every
            // attempt stores one answer per question id, and its score was worked out
            // from the wording that was on screen at the time. Only a draft is safe
            // to reshape - nobody can have played it yet.
            if (quiz.getStatus() != QuizStatus.DRAFT) {
                throw new ConflictException(
                        "Questions can only be changed while the quiz is a DRAFT - this one is " + quiz.getStatus());
            }
            // orphanRemoval deletes the rows dropped here; addQuestion() then renumbers
            // the positions from 1 again.
            quiz.getQuestions().clear();
            for (CreateQuestionRequest createQuestionRequest : newQuestions) {
                quiz.addQuestion(MAPPER.createRequestToQuestion(createQuestionRequest));
            }
        }

        // Flush before mapping: the new questions are only given their ids when the
        // insert actually runs, and that would otherwise happen after the response
        // has been built - handing the client a list of nulls.
        Quiz saved = quizRepository.saveAndFlush(quiz);

        log.info("User {} updated quiz id={} ({} questions)", username, quizId, saved.getQuestions().size());

        return MAPPER.quizToDetail(saved);
    }

    @Override
    @Transactional
    public void delete(Long quizId, String username) {
        Quiz quiz = loadQuiz(quizId);
        requireOwner(quiz, username);

        // attempt_answer.question_id has no ON DELETE rule, so dropping the questions
        // out from under a played quiz fails in the database anyway. Deleting is for
        // mistakes; a quiz people have actually played gets closed instead, and its
        // leaderboard survives.
        long plays = attemptRepository.countByQuizId(quizId);
        if (plays > 0) {
            throw new ConflictException(plays + (plays == 1 ? " person has" : " people have")
                    + " already played this quiz - close it instead of deleting it");
        }

        quizRepository.delete(quiz);

        log.info("User {} deleted quiz id={}", username, quizId);
    }

    @Override
    public List<QuizSummaryResponse> findMine(String username) {
        User owner = userService.requireByUsername(username);

        return MAPPER.quizzesToSummaries(quizRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId()));
    }

    @Override
    public List<QuizSummaryResponse> findPublished() {
        return MAPPER.quizzesToSummaries(quizRepository.findByStatusOrderByCreatedAtDesc(QuizStatus.PUBLISHED));
    }

    @Override
    public QuizDetailResponse findDetailForOwner(Long quizId, String username) {
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId).orElseThrow(()-> NotFoundException.of("Quiz", quizId));


        requireOwner(quiz, username);
        return MAPPER.quizToDetail(quiz);
    }

    @Override
    @Transactional
    public QuizSummaryResponse publish(Long quizId, String username) {
         Quiz quiz  = loadQuiz(quizId);
         requireOwner(quiz, username);
         quiz.setStatus(QuizStatus.PUBLISHED);
        return MAPPER.quizSummaryResponse(quiz);
    }

    @Override
    @Transactional
    public QuizSummaryResponse close(Long quizId, String username) {
        Quiz quiz  = loadQuiz(quizId);
        requireOwner(quiz, username);
        quiz.setStatus(QuizStatus.CLOSED);
        return MAPPER.quizSummaryResponse(quiz);
    }


    private Quiz loadQuiz(Long quizId ) {
        return quizRepository.findById(quizId).orElseThrow(()-> NotFoundException.of("Quiz", quizId));
    }

    /**
     * OWNERSHIP check. SecurityConfig can only answer "which role are you";
     * only this layer knows who owns row 42, so the check belongs here.
     */

    private void requireOwner(Quiz quiz, String username) {
        if(!quiz.getOwner().getUsername().equals(username)) {
            throw new ForbiddenException("Quiz " + quiz.getId() + " does not belong to you");

        }
    }


}
