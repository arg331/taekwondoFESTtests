package com.taekwondo.examenes.user;

/**
 * Rol de un usuario.
 * <ul>
 *   <li>ADMIN: todo lo del profesor, más gestionar usuarios y roles.</li>
 *   <li>TEACHER: banco de preguntas, exámenes y resultados.</li>
 *   <li>STUDENT: hace exámenes y ve su historial.</li>
 * </ul>
 */
public enum UserRole {
    ADMIN,
    TEACHER,
    STUDENT
}
