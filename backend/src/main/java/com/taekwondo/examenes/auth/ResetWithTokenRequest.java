package com.taekwondo.examenes.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Poner una contraseña nueva con el código del enlace del correo (RF-34). */
public record ResetWithTokenRequest(
        @NotBlank @Size(max = 100) String token,
        @NotBlank @Size(min = 6, max = 100) String newPassword
) {}
