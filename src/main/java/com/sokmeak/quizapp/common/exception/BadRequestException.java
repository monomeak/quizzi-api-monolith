package com.sokmeak.quizapp.common.exception;

/** 400 - the request itself does not make sense. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
