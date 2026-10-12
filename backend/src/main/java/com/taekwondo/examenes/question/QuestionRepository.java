package com.taekwondo.examenes.question;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    List<Question> findAllByOwnerId(Long ownerId);

    List<Question> findAllByIdInAndOwnerId(Collection<Long> ids, Long ownerId);

    Optional<Question> findByIdAndOwnerId(Long id, Long ownerId);
}
