package com.sokmeak.quizapp.modules.quiz.dto.response;

public record QuestionResponse (
        Long id,
        int position,
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption,
        String difficulty
){

}
