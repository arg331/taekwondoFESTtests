package com.taekwondo.examenes.infrastructure.web.dto;

/**
 * DTO HTTP de entrada para crear un tag.
 *
 * Separado de CreateTagInput (aplicación) deliberadamente: las dos
 * clases tienen los mismos campos hoy, pero pueden divergir mañana.
 * Esta separación protege la aplicación de cambios en el formato HTTP.
 *
 * El ownerId NO viene aquí: el controller lo obtiene del usuario
 * autenticado (JWT). El cliente no debe poder elegir el dueño
 * de los tags que crea.
 */
public record CreateTagRequest(
        String name,
        String color
) {}
