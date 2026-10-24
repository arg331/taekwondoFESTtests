package com.taekwondo.examenes.attempt;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Cómo se reconoce a un alumno entre entregas e intentos (RN-08, RF-54): un registrado por su cuenta y
 * un anónimo por su nombre, sin distinguir mayúsculas, tildes ni espacios de más.
 */
public final class StudentIdentity {

    private StudentIdentity() {
    }

    /** «  José  PÉREZ » → «jose perez». */
    public static String nameKey(String name) {
        String withoutAccents = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** Clave del alumno de una entrega: «u:<id>» si está registrado, «n:<nombre>» si es anónimo. */
    public static String key(Long studentUserId, String studentName) {
        return studentUserId != null ? "u:" + studentUserId : "n:" + nameKey(studentName);
    }
}
