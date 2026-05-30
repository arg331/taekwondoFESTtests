package com.taekwondo.examenes.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad de dominio: ExamFavorite.
 *
 * Representa que un profesor ha marcado como favorito el examen
 * (público) de OTRO profesor. Es una relación con identidad propia.
 *
 * Esta entidad NO sabe si el examen referenciado es público o si
 * el profesorId es distinto del owner del examen. Eso son reglas
 * de coordinación que validan los casos de uso, no la entidad.
 * La entidad solo garantiza sus invariantes locales: profesorId y
 * examId no nulos.
 */
public final class ExamFavorite {

    private final Long id;
    private final Long professorId;
    private final Long examId;
    private final LocalDateTime createdAt;

    public static ExamFavorite createNew(Long professorId, Long examId) {
        validateProfessorId(professorId);
        validateExamId(examId);
        return new ExamFavorite(null, professorId, examId, LocalDateTime.now());
    }

    public static ExamFavorite reconstitute(Long id,
                                              Long professorId,
                                              Long examId,
                                              LocalDateTime createdAt) {
        Objects.requireNonNull(id, "id no puede ser null en reconstitución");
        return new ExamFavorite(id, professorId, examId, createdAt);
    }

    private ExamFavorite(Long id, Long professorId, Long examId, LocalDateTime createdAt) {
        this.id = id;
        this.professorId = professorId;
        this.examId = examId;
        this.createdAt = createdAt;
    }

    private static void validateProfessorId(Long professorId) {
        Objects.requireNonNull(professorId, "professorId no puede ser null");
    }

    private static void validateExamId(Long examId) {
        Objects.requireNonNull(examId, "examId no puede ser null");
    }

    public Long getId()                  { return id; }
    public Long getProfessorId()         { return professorId; }
    public Long getExamId()              { return examId; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExamFavorite f)) return false;
        return Objects.equals(id, f.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
