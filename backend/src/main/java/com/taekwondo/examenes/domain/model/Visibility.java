package com.taekwondo.examenes.domain.model;

/**
 * Visibilidad de un examen frente a otros profesores.
 *
 *  PRIVATE → solo lo ve su dueño
 *  PUBLIC  → otros profesores pueden verlo, favoritearlo, importarlo
 */
public enum Visibility {
    PRIVATE,
    PUBLIC
}