package com.taekwondo.examenes.domain.port;

/**
 * Puerto de salida: generador de códigos únicos para exámenes.
 *
 * El código generado se usa como identificador público del examen
 * (el que aparece en el QR y permite a los estudiantes acceder).
 *
 * Existe como puerto para:
 *  - Aislar al dominio de la estrategia concreta de generación
 *    (UUID, secuencial, hash, etc.).
 *  - Permitir tests con códigos predecibles (no aleatorios).
 *  - Cambiar la estrategia sin tocar los casos de uso.
 *
 * El contrato NO garantiza unicidad absoluta entre llamadas: si el sistema
 * lo necesita, el caso de uso que invoca este generador debe comprobarlo
 * contra el repositorio (ExamRepository.existsByCode).
 */
public interface ExamCodeGenerator {

    /**
     * @return un código nuevo, formato dependiente de la implementación
     *         (ejemplo de producción: "EXM-A1B2C3D4").
     */
    String generate();
}