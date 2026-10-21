package com.taekwondo.examenes.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cambiar el color de un tag (RF-08). */
public record ChangeTagColorRequest(
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "debe ser un color hex válido (ej: #3B8BD4)")
        String newColor
) {}
