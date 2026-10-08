package com.taekwondo.examenes.entity;

/**
 * Quién puede entregar el examen: cualquiera (OPEN) o solo usuarios con sesión (REGISTERED_ONLY).
 */
public enum ExamAccessMode {
    OPEN,
    REGISTERED_ONLY
}
