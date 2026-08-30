package com.sokmeak.quizapp.utils;

/**
 * How one answer turned out when the player reviews an attempt.
 * SKIPPED is scored exactly like INCORRECT - it is kept apart only so the
 * review screen can say "you left this one blank" instead of "you got it wrong".
 */
public enum AnswerVerdict {
    CORRECT,
    INCORRECT,
    SKIPPED
}
