package com.taekwondo.examenes.exam;

import jakarta.validation.constraints.NotBlank;

/** Recuperar el intento tras recargar la página o perder la conexión (RF-24). */
public record ResumeAttemptRequest(@NotBlank String attemptToken) {}
