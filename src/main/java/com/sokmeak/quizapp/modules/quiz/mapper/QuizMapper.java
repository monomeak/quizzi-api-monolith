package com.sokmeak.quizapp.modules.quiz.mapper;


import com.sokmeak.quizapp.common.exception.BadRequestException;
import com.sokmeak.quizapp.modules.question.entity.Question;
import com.sokmeak.quizapp.modules.quiz.dto.request.CreateQuestionRequest;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuestionResponse;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizDetailResponse;
import com.sokmeak.quizapp.modules.quiz.dto.response.QuizSummaryResponse;
import com.sokmeak.quizapp.modules.quiz.entity.Quiz;
import com.sokmeak.quizapp.utils.DifficultyLevel;
import com.sokmeak.quizapp.utils.OptionKey;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface QuizMapper {
    QuizMapper INSTANCE = Mappers.getMapper(QuizMapper.class);

    @Mapping(source = "owner.username", target = "ownerUsername")
    @Mapping(target = "questionCount", expression = "java(quiz.getQuestions() == null ? 0 : quiz.getQuestions().size())")
    QuizSummaryResponse quizSummaryResponse(Quiz quiz);

    List<QuizSummaryResponse> quizzesToSummaries(List<Quiz> quizzes);

    @Mapping(source = "owner.username", target = "ownerUsername")
    QuizDetailResponse quizToDetail(Quiz quiz);

    QuestionResponse quizToQuestionResponse(Quiz quiz);

    List<QuestionResponse> questionsToResponses(List<Question> questions);

    /*
quiz and position are set by Quiz.addQuestion(), not by the mapper.
     *
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    @Mapping(target = "position", ignore = true)
    Question createRequestToQuestion(CreateQuestionRequest request);

    List<Question> createRequestsToQuestions(List<CreateQuestionRequest> requests);


    /** Custom conversion: "a" and "A" both work, anything else is a clean 400. */
    default OptionKey toOptionKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OptionKey.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("correctOption must be A, B, C or D but was '" + value + "'");
        }
    }

    default DifficultyLevel toDifficultyLevel(String value) {
        if (value == null || value.isBlank()) {
            return DifficultyLevel.EASY; // sensible default
        }
        try {
            return DifficultyLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("difficulty must be EASY, MEDIUM or HARD but was '" + value + "'");
        }
    }









}



