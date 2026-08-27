package com.sokmeak.quizapp.modules.auth.service;

import com.sokmeak.quizapp.modules.auth.dto.request.LoginRequest;
import com.sokmeak.quizapp.modules.auth.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
}
