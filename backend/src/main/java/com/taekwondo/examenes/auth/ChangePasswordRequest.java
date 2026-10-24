package com.taekwondo.examenes.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambiar la propia contraseña (RF-06): hay que dar la actual. */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 6, max = 100) String newPassword
) {}
