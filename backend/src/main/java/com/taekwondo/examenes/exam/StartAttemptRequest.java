package com.taekwondo.examenes.exam;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El alumno se identifica al empezar (DE-04); la entrega queda a su nombre. */
public record StartAttemptRequest(
        @NotBlank @Size(max = 100) String studentName,
        @Size(max = 100) String studentClub,
        @Email @Size(max = 200) String studentEmail
) {}
