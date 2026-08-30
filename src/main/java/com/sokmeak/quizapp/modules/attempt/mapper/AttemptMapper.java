package com.sokmeak.quizapp.modules.attempt.mapper;

import com.sokmeak.quizapp.modules.attempt.dto.response.AttemptHistoryResponse;
import com.sokmeak.quizapp.modules.attempt.dto.response.AttemptQuestionResponse;
import com.sokmeak.quizapp.modules.attempt.entities.QuizAttempt;
import com.sokmeak.quizapp.modules.question.entity.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface AttemptMapper {
    AttemptMapper INSTANCE = Mappers.getMapper(AttemptMapper.class);

    @Mapping(source = "id", target = "questionId")
    AttemptQuestionResponse questionToAttemptQuestion(Question question);

    List<AttemptQuestionResponse> questionsToAttemptQuestions(List<Question> questions);

    //mapping some field
    @Mapping(source = "id", target = "attemptId")
    @Mapping(source = "quiz.id", target = "quizId")
    @Mapping(source = "quiz.title", target = "quizTitle")
    @Mapping(source = "quiz.category", target = "category")
    AttemptHistoryResponse attemptToHistory(QuizAttempt attempt);

    List<AttemptHistoryResponse> attemptsToHistory(List<QuizAttempt> attempts);

}
