package com.taekwondo.examenes.exam;

import java.time.LocalDateTime;

/** Nueva fecha de expiración al reabrir o ampliar un examen. null = sin expiración. */
public record ExpirationRequest(LocalDateTime newExpiresAt) {}
