package com.taekwondo.examenes.question;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    /** El banco del profesor, sin las eliminadas. */
    List<Question> findAllByOwnerIdAndDeletedAtIsNull(Long ownerId);

    /** Las eliminadas, de la última a la primera. */
    List<Question> findAllByOwnerIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Long ownerId);

    List<Question> findAllByIdInAndOwnerIdAndDeletedAtIsNull(Collection<Long> ids, Long ownerId);

    Optional<Question> findByIdAndOwnerIdAndDeletedAtIsNull(Long id, Long ownerId);

    Optional<Question> findByIdAndOwnerId(Long id, Long ownerId);
}
