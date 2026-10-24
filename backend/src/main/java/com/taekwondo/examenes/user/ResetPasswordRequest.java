package com.taekwondo.examenes.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contraseña temporal que el administrador pone a un usuario (RF-35). */
public record ResetPasswordRequest(@NotBlank @Size(min = 6, max = 100) String temporaryPassword) {}
