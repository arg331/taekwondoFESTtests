package com.taekwondo.examenes.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "solo letras, números y guion bajo")
        String username,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(min = 6, max = 100) String plainPassword,
        @NotBlank @Size(max = 100) String displayName
) {}
