package com.taekwondo.examenes.infrastructure.persistence.mapper;

import com.taekwondo.examenes.domain.model.Answer;
import com.taekwondo.examenes.domain.model.Result;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.AnswerJpaEmbeddable;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.ResultJpaEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversión bidireccional entre Result (dominio) y ResultJpaEntity (JPA).
 *
 * Como Result es inmutable, este mapper se usa principalmente:
 *  - toJpa: cuando se persiste un Result nuevo
 *  - toDomain: cuando se lee de la BD (usa reconstitute, que confía
 *    en los valores precalculados de score, correctAnswers, etc.)
 *
 * No hay coordinación con otros agregados (a diferencia de ExamMapper
 * con TagJpaEntity): Result solo guarda IDs hacia Question y User.
 */
public final class ResultMapper {

    private ResultMapper() {}

    // ──────────────────────────────────────────────
    // JPA → dominio
    // ──────────────────────────────────────────────

    public static Result toDomain(ResultJpaEntity entity) {
        List<Answer> answers = entity.getAnswers().stream()
                .map(ResultMapper::answerToDomain)
                .toList();

        return Result.reconstitute(
                entity.getId(),
                entity.getExamId(),
                entity.getStudentUserId(),
                entity.getStudentName(),
                entity.getStudentClub(),
                entity.getStudentEmail(),
                answers,
                entity.getCorrectAnswers(),
                entity.getTotalQuestions(),
                entity.getScore(),
                entity.getTimeSpentSeconds(),
                entity.getCompletedAt()
        );
    }

    private static Answer answerToDomain(AnswerJpaEmbeddable embedded) {
        return Answer.of(
                embedded.getQuestionId(),
                embedded.getStudentAnswer(),
                embedded.getCorrectAnswer()
        );
    }

    // ──────────────────────────────────────────────
    // Dominio → JPA
    // ──────────────────────────────────────────────

    public static ResultJpaEntity toJpa(Result result) {
        ResultJpaEntity entity = new ResultJpaEntity();
        entity.setId(result.getId());
        entity.setExamId(result.getExamId());
        entity.setStudentUserId(result.getStudentUserId());
        entity.setStudentName(result.getStudentName());
        entity.setStudentClub(result.getStudentClub());
        entity.setStudentEmail(result.getStudentEmail());
        entity.setAnswers(answersToJpa(result.getAnswers()));
        entity.setCorrectAnswers(result.getCorrectAnswers());
        entity.setTotalQuestions(result.getTotalQuestions());
        entity.setScore(result.getScore());
        entity.setTimeSpentSeconds(result.getTimeSpentSeconds());
        entity.setCompletedAt(result.getCompletedAt());
        return entity;
    }

    private static List<AnswerJpaEmbeddable> answersToJpa(List<Answer> answers) {
        List<AnswerJpaEmbeddable> result = new ArrayList<>(answers.size());
        for (Answer a : answers) {
            result.add(new AnswerJpaEmbeddable(
                    a.getQuestionId(),
                    a.getStudentAnswer(),
                    a.getCorrectAnswer()
            ));
        }
        return result;
    }
}
