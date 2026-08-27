package com.sokmeak.quizapp.common.exception;

/**
 * 409 - the request is valid but clashes with the current state:
 * username already taken, quiz already submitted, and so on.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
