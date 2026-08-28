package com.sokmeak.quizapp.modules.attempt.dto.response;

/**
 * One question as it looked to the player, plus what they picked and what was
 * right. Only ever returned for an attempt that is already SUBMITTED, so
 * handing the correct answer back cannot help anyone cheat.
 */
public record AnswerReviewResponse(
        Long questionId,
        int position,
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        /** The letter the player chose, or null when they skipped the question. */
        String selectedOption,
        /** The text behind that letter, so the client does not have to look it up. */
        String selectedOptionText,
        String correctOption,
        String correctOptionText,
        boolean correct,
        /** CORRECT, INCORRECT or SKIPPED. */
        String verdict,
        /** Ready-to-show line: "Well done!" or the correct answer spelled out. */
        String feedback
) {
}
