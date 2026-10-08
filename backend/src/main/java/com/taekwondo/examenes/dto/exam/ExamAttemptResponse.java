package com.taekwondo.examenes.dto.exam;

import com.taekwondo.examenes.dto.question.PublicQuestionResponse;

import java.util.List;

/**
 * Inicio de un intento de examen. El alumno debe devolver attemptToken al
 * entregar: lleva firmada la hora de inicio para controlar el tiempo límite.
 */
public record ExamAttemptResponse(String attemptToken, List<PublicQuestionResponse> questions) {}
