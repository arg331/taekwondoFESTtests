package com.taekwondo.examenes.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pedir sin sesión el usuario y un enlace para la contraseña (RF-34). */
public record RecoveryRequest(@NotBlank @Email @Size(max = 200) String email) {}
