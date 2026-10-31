package com.taekwondo.examenes.exam;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findByCode(String code);

    Optional<Exam> findByIdAndOwnerId(Long id, Long ownerId);

    List<Exam> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<Exam> findAllByVisibilityAndStatusAndOwnerIdNot(Visibility visibility, ExamStatus status, Long ownerId);

    /** Exámenes de prueba de otros profesores (RF-30). */
    List<Exam> findAllByVisibilityAndStatusInAndOwnerIdNotOrderByCreatedAtDesc(Visibility visibility,
                                                                             Collection<ExamStatus> statuses, Long ownerId);

    boolean existsByCode(String code);

    /**
     * Bloquea la fila del examen hasta que acaba la transacción. Al empezar un intento, dos peticiones a la vez
     * del mismo alumno (doble clic, varias pestañas) se ponen en fila: la segunda ya ve el intento de la primera.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exam e where e.id = :id")
    Optional<Exam> lockById(@Param("id") Long id);

    @Query("select e from Exam e join e.questionIds q where q = :questionId")
    List<Exam> findAllContainingQuestion(@Param("questionId") Long questionId);

    /** Cada pregunta con el número de exámenes que la usan, borradores incluidos; las que no se usan no salen. */
    @Query("select q, count(distinct e) from Exam e join e.questionIds q where q in :questionIds group by q")
    List<Object[]> countExamsByQuestion(@Param("questionIds") Collection<Long> questionIds);
}
