package com.taekwondo.examenes.dto.auth;

public record LoginResponse(String token, UserResponse user) {}
