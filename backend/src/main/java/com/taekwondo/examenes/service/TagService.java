package com.taekwondo.examenes.service;

import com.taekwondo.examenes.dto.tag.CreateTagRequest;
import com.taekwondo.examenes.dto.tag.RenameTagRequest;
import com.taekwondo.examenes.dto.tag.TagResponse;
import com.taekwondo.examenes.entity.Tag;
import com.taekwondo.examenes.exception.BusinessRuleException;
import com.taekwondo.examenes.exception.ResourceNotFoundException;
import com.taekwondo.examenes.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public TagResponse create(CreateTagRequest request, Long ownerId) {
        String name = request.name().trim();
        if (tagRepository.existsByNameIgnoreCaseAndOwnerId(name, ownerId)) {
            throw new BusinessRuleException("Ya existe un tag con el nombre '" + name + "'");
        }
        return TagResponse.from(tagRepository.save(new Tag(name, request.color(), ownerId)));
    }

    public TagResponse rename(Long tagId, RenameTagRequest request, Long ownerId) {
        Tag tag = getOwned(tagId, ownerId);
        String newName = request.newName().trim();
        if (!tag.getName().equalsIgnoreCase(newName)
                && tagRepository.existsByNameIgnoreCaseAndOwnerId(newName, ownerId)) {
            throw new BusinessRuleException("Ya existe un tag con el nombre '" + newName + "'");
        }
        tag.rename(newName);
        return TagResponse.from(tag);
    }

    @Transactional(readOnly = true)
    public TagResponse get(Long tagId, Long ownerId) {
        return TagResponse.from(getOwned(tagId, ownerId));
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list(Long ownerId) {
        return tagRepository.findAllByOwnerIdOrderByNameAsc(ownerId).stream()
                .map(TagResponse::from)
                .toList();
    }

    /**
     * Carga los tags indicados comprobando que todos existen y son del profesor.
     * Usado al asignar tags a preguntas y al pre-generar exámenes.
     */
    @Transactional(readOnly = true)
    public Set<Tag> findOwnedTags(Set<Long> tagIds, Long ownerId) {
        if (tagIds == null || tagIds.isEmpty()) return new HashSet<>();
        List<Tag> tags = tagRepository.findAllByIdInAndOwnerId(tagIds, ownerId);
        if (tags.size() != tagIds.size()) {
            throw new BusinessRuleException("Alguno de los tags indicados no existe");
        }
        return new HashSet<>(tags);
    }

    private Tag getOwned(Long tagId, Long ownerId) {
        return tagRepository.findByIdAndOwnerId(tagId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un tag con id " + tagId));
    }
}
