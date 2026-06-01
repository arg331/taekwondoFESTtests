package com.taekwondo.examenes.domain.model;

/**
 * Modo de acceso a un examen, decidido al publicarlo.
 *
 *  OPEN              → cualquiera con el código puede hacerlo (anónimo o registrado)
 *  REGISTERED_ONLY   → solo estudiantes registrados pueden hacerlo
 *
 * En ambos modos el acceso siempre es por código (el del QR).
 * El frontend mostrará la pantalla de login si OPEN y el usuario quiere
 * registrarse, o si REGISTERED_ONLY y no está logueado.
 */
public enum ExamAccessMode {
    OPEN,
    REGISTERED_ONLY
}
