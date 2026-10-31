package com.taekwondo.examenes.community;

/** Cuántas preguntas han pasado a mi banco y cuántas no, porque ya tenía una igual. */
public record CopyQuestionsResponse(int copied, int alreadyInBank) {}
