package com.sokmeak.quizapp.common.exception;
/**
 * 403 - you are logged in, but this object is not yours.
 * Role checks live in SecurityConfig; ownership checks live in the services,
 * because only the service knows who owns row 42.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
