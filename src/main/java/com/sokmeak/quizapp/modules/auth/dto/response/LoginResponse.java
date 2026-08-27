package com.sokmeak.quizapp.modules.auth.dto.response;

import com.sokmeak.quizapp.modules.user.dto.UserResponse;

/**
 * What a client needs after a successful login, and nothing more.
 *
 * The profile is bundled in so a front-end does not have to call
 * GET /api/v1/users/me straight after logging in.
 *
 * @param accessToken the signed JWT - send it back as "Authorization: Bearer <token>"
 * @param tokenType   always "Bearer"; part of the response so clients can build the header blindly
 * @param expiresIn   lifetime in SECONDS, so a client can refresh before it lapses
 * @param user        the profile of whoever just logged in
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
