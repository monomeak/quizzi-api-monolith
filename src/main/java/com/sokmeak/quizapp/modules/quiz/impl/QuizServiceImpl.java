package com.sokmeak.quizapp.modules.quiz.impl;

import com.sokmeak.quizapp.common.exception.ForbiddenException;
import com.sokmeak.quizapp.common.exception.NotFoundException;
import com.sokmeak.quizapp.modules.quiz.dto.request.CreateQuestionRequest;
import com.sokmeak.quizapp.modules.quiz.dto.request.CreateQuizRequest;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizDetailResponse;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizSummaryResponse;
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
