package com.taekwondo.examenes.domain.model;

/**
 * Estado del ciclo de vida de un examen.
 *
 *  DRAFT      → en edición, modificable por el profesor
 *  PUBLISHED  → publicado, los estudiantes pueden hacerlo, INMUTABLE
 *  EXPIRED    → publicado pero su fecha de expiración ya pasó
 */
public enum ExamStatus {
    DRAFT,
    PUBLISHED,
    EXPIRED
}