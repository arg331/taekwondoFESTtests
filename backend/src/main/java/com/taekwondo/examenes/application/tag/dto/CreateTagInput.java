package com.taekwondo.examenes.application.tag.dto;

/**
 * Datos necesarios para crear un tag.
 *
 * Record inmutable. NO contiene lógica de negocio: solo transporta datos.
 * Las validaciones de formato (nombre no vacío, color hex válido, etc.)
 * las hace la entidad Tag al construirse.
 */
public record CreateTagInput(
        String name,
        String color,
        Long ownerId
) {}