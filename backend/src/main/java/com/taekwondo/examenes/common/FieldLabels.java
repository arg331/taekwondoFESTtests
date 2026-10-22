package com.taekwondo.examenes.common;

import java.util.Map;

/**
 * Nombre de cada campo tal como lo ve el usuario, para los errores de validación (RNF-13):
 * «Contraseña: el tamaño debe estar entre 6 y 100» en vez de «plainPassword: …».
 */
public final class FieldLabels {

    private static final Map<String, String> LABELS = Map.ofEntries(
            // Cuentas
            Map.entry("username", "Usuario"),
            Map.entry("usernameOrEmail", "Usuario o email"),
            Map.entry("email", "Email"),
            Map.entry("displayName", "Nombre"),
            Map.entry("plainPassword", "Contraseña"),
            Map.entry("currentPassword", "Contraseña actual"),
            Map.entry("newPassword", "Contraseña nueva"),
            Map.entry("temporaryPassword", "Contraseña temporal"),
            Map.entry("token", "Enlace"),
            Map.entry("role", "Rol"),
            Map.entry("active", "Activa"),
            Map.entry("userId", "Usuario"),
            Map.entry("clubId", "Club"),
            // Clubes y tags
            Map.entry("name", "Nombre"),
            Map.entry("newName", "Nombre nuevo"),
            Map.entry("color", "Color"),
            Map.entry("newColor", "Color"),
            // Preguntas
            Map.entry("text", "Texto de la pregunta"),
            Map.entry("options", "Opciones"),
            Map.entry("correctAnswers", "Respuestas correctas"),
            Map.entry("correctAnswersValid", "Respuestas correctas"),
            Map.entry("explanation", "Explicación"),
            Map.entry("grade", "Grado"),
            Map.entry("type", "Tipo"),
            Map.entry("tagIds", "Tags"),
            // Exámenes
            Map.entry("title", "Título"),
            Map.entry("newTitle", "Título"),
            Map.entry("numberOfQuestions", "Número de preguntas"),
            Map.entry("timeLimitMinutes", "Tiempo límite"),
            Map.entry("visibility", "Tipo de examen"),
            Map.entry("newVisibility", "Tipo de examen"),
            Map.entry("accessMode", "Quién puede hacerlo"),
            Map.entry("maxAttempts", "Intentos"),
            Map.entry("expiresAt", "Fecha de expiración"),
            Map.entry("newExpiresAt", "Fecha de expiración"),
            Map.entry("questionIds", "Preguntas"),
            Map.entry("requiredAnyOfTagIds", "Tags"),
            Map.entry("teacherId", "Profesor"),
            // Hacer el examen
            Map.entry("studentName", "Nombre"),
            Map.entry("studentClub", "Club"),
            Map.entry("studentEmail", "Email"),
            Map.entry("examCode", "Código del examen"),
            Map.entry("attemptToken", "Intento"),
            Map.entry("answers", "Respuestas"),
            Map.entry("questionId", "Pregunta"),
            Map.entry("chosenOptions", "Opciones marcadas"));

    private FieldLabels() {
    }

    /** «answers[0].chosenOptions[1]» → «Opciones marcadas»; un campo sin nombre conocido se deja como está. */
    public static String of(String field) {
        String last = field.substring(field.lastIndexOf('.') + 1).replaceAll("\\[\\d*]", "");
        return LABELS.getOrDefault(last, field);
    }
}
