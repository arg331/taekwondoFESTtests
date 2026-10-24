package com.taekwondo.examenes.exam;

import com.taekwondo.examenes.question.PublicQuestionResponse;

import java.util.List;
import java.util.Map;

/**
 * Intento de examen, nuevo o recuperado (RF-24). El alumno debe devolver attemptToken al guardar
 * respuestas y al entregar: identifica el intento registrado. attemptNumber de maxAttempts dice cuál
 * es (RF-52); sin límite, maxAttempts es null. answers trae lo ya marcado en cada pregunta (vacío =
 * sin responder) y secondsLeft, el tiempo que le queda (null sin tiempo límite).
 */
public record ExamAttemptResponse(String attemptToken, List<PublicQuestionResponse> questions,
                                  int attemptNumber, Integer maxAttempts,
                                  Map<Long, List<Integer>> answers, Long secondsLeft) {}
