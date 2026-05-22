package com.taekwondo.examenes.application.tag.dto;

/**
 * Datos para renombrar un tag.
 *
 * Incluye el requesterOwnerId para que el caso de uso pueda verificar
 * que el solicitante es el dueño del tag. El controller lo rellenará
 * a partir del usuario autenticado (JWT, sesión, etc.), no del cuerpo
 * de la petición HTTP.
 */
public record RenameTagInput(
        Long tagId,
        String newName,
        Long requesterOwnerId
) {}