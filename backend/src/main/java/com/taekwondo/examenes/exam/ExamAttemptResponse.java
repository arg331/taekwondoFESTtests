package com.taekwondo.examenes.exam;

import com.taekwondo.examenes.question.PublicQuestionResponse;
import java.util.List;

/**
 * Inicio de un intento de examen. El alumno debe devolver attemptToken al entregar:
 * identifica el intento registrado. attemptNumber de maxAttempts dice cuál es (RF-52).
 */
public record ExamAttemptResponse(String attemptToken, List<PublicQuestionResponse> questions,
                                  int attemptNumber, int maxAttempts) {}
