package com.sokmeak.quizapp.modules.user.service;

import com.sokmeak.quizapp.modules.auth.dto.request.RegisterUserRequest;
import com.sokmeak.quizapp.modules.user.dto.UserResponse;
import com.sokmeak.quizapp.modules.user.entity.User;

public interface UserService {


    UserResponse register(RegisterUserRequest request);

    UserResponse findByUsername(String username);
    /** Used by the other services when they need the entity, not the DTO. */
    User requireByUsername(String username);
}
