package com.taekwondo.examenes.infrastructure.web.dto;

/**
 * DTO HTTP para el registro público de un estudiante.
 *
 * El rol no se acepta: el caso de uso siempre crea STUDENT.
 * La password viaja en claro por HTTP — debe usarse HTTPS en producción.
 */
public record RegisterStudentRequest(
        String username,
        String email,
        String plainPassword,
        String displayName
) {}
