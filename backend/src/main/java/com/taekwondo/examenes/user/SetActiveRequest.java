package com.taekwondo.examenes.user;

import jakarta.validation.constraints.NotNull;

/** Desactivar (false) o reactivar (true) una cuenta (RF-05). */
public record SetActiveRequest(@NotNull Boolean active) {}
