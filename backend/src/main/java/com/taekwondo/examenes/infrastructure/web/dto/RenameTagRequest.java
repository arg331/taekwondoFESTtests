package com.taekwondo.examenes.infrastructure.web.dto;

/**
 * DTO HTTP de entrada para renombrar un tag.
 *
 * El tagId viene en la URL (path param), no en el body.
 * El requesterOwnerId lo añade el controller desde el usuario autenticado.
 */
public record RenameTagRequest(
        String newName
) {}
