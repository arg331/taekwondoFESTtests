package com.taekwondo.examenes.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findAllByOwnerIdOrderByNameAsc(Long ownerId);

    List<Tag> findAllByIdInAndOwnerId(Collection<Long> ids, Long ownerId);

    Optional<Tag> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByNameIgnoreCaseAndOwnerId(String name, Long ownerId);

    /** Quita el tag de las preguntas que lo tienen, antes de borrarlo (RF-08). */
    @Modifying
    @Query(value = "delete from question_tags where tag_id = :tagId", nativeQuery = true)
    void detachFromQuestions(Long tagId);

    /** Quita el tag de los exámenes generados con él, antes de borrarlo (RF-08). */
    @Modifying
    @Query(value = "delete from exam_generation_tags where tag_id = :tagId", nativeQuery = true)
    void detachFromExams(Long tagId);
}
