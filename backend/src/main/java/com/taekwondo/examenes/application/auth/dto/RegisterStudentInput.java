package com.taekwondo.examenes.application.auth.dto;

/**
 * Datos para el registro libre de un estudiante.
 *
 * Es diferente de CreateUserInput porque:
 *  - Aquí cualquier visitante puede registrarse (endpoint público).
 *  - El rol siempre será STUDENT, no se acepta como parámetro.
 *
 * Mantener ambos DTOs separados, aunque sus campos coincidan, refleja
 * que son dos casos de uso conceptualmente distintos.
 */
public record RegisterStudentInput(
        String username,
        String email,
        String plainPassword,
        String displayName
) {}
