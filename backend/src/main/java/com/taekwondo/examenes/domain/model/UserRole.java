package com.taekwondo.examenes.domain.model;

/**
 * Rol de un usuario en el sistema.
 *
 *  ADMIN   → profesor: crea preguntas, exámenes, ve resultados
 *  STUDENT → estudiante registrado: hace exámenes, ve su historial
 *
 * Los usuarios anónimos NO son un rol: simplemente no tienen cuenta.
 */
public enum UserRole {
    ADMIN,
    STUDENT
}
