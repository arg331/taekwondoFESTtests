package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findAllByOwnerIdOrderByNameAsc(Long ownerId);

    List<Tag> findAllByIdInAndOwnerId(Collection<Long> ids, Long ownerId);

    Optional<Tag> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByNameIgnoreCaseAndOwnerId(String name, Long ownerId);
}
