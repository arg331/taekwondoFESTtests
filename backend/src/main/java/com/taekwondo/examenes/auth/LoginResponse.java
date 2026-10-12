package com.taekwondo.examenes.auth;

import com.taekwondo.examenes.user.UserResponse;

public record LoginResponse(String token, UserResponse user) {}
