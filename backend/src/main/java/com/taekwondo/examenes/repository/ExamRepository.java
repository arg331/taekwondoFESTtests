package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.entity.Exam;
import com.taekwondo.examenes.entity.ExamStatus;
import com.taekwondo.examenes.entity.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findByCode(String code);

    Optional<Exam> findByIdAndOwnerId(Long id, Long ownerId);

    List<Exam> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<Exam> findAllByVisibilityAndStatusAndOwnerIdNot(Visibility visibility, ExamStatus status, Long ownerId);

    boolean existsByCode(String code);

    @Query("select e from Exam e join e.questionIds q where q = :questionId")
    List<Exam> findAllContainingQuestion(@Param("questionId") Long questionId);
}
