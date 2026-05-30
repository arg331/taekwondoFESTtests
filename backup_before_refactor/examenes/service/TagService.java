package com.taekwondo.examenes.service;

import com.taekwondo.examenes.dto.TagDTO;
import com.taekwondo.examenes.exception.ResourceNotFoundException;
import com.taekwondo.examenes.model.Tag;
import com.taekwondo.examenes.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de Tags
 * 
 * REGLA IMPORTANTE: Los tags NO se pueden eliminar, solo renombrar
 */
@Service
@RequiredArgsConstructor
public class TagService {
    
    private final TagRepository tagRepository;
    
    /**
     * Obtener todos los tags de un profesor
     */
    public List<TagDTO> getAllByOwnerId(Long ownerId) {
        return tagRepository.findByOwnerId(ownerId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtener un tag por ID
     */
    public TagDTO getById(Long id, Long ownerId) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tag no encontrado con id: " + id));
        
        // Verificar que el tag pertenece al profesor
        if (!tag.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("Tag no encontrado");
        }
        
        return convertToDTO(tag);
    }
    
    /**
     * Crear nuevo tag
     */
    @Transactional
    public TagDTO create(TagDTO tagDTO, Long ownerId) {
        // Validar que no exista un tag con el mismo nombre para este profesor
        if (tagRepository.existsByNameAndOwnerId(tagDTO.getName(), ownerId)) {
            throw new IllegalArgumentException("Ya existe un tag con el nombre: " + tagDTO.getName());
        }
        
        Tag tag = new Tag();
        tag.setName(tagDTO.getName());
        tag.setColor(tagDTO.getColor());
        tag.setOwnerId(ownerId);
        
        Tag saved = tagRepository.save(tag);
        return convertToDTO(saved);
    }
    
    /**
     * Actualizar tag (solo nombre y color, NO se puede cambiar el owner)
     * IMPORTANTE: Solo se puede RENOMBRAR, no eliminar
     */
    @Transactional
    public TagDTO update(Long id, TagDTO tagDTO, Long ownerId) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tag no encontrado con id: " + id));
        
        // Verificar que el tag pertenece al profesor
        if (!tag.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("Tag no encontrado");
        }
        
        // Si se cambia el nombre, verificar que no exista otro tag con ese nombre
        if (!tag.getName().equals(tagDTO.getName())) {
            if (tagRepository.existsByNameAndOwnerId(tagDTO.getName(), ownerId)) {
                throw new IllegalArgumentException("Ya existe un tag con el nombre: " + tagDTO.getName());
            }
        }
        
        tag.setName(tagDTO.getName());
        tag.setColor(tagDTO.getColor());
        
        Tag updated = tagRepository.save(tag);
        return convertToDTO(updated);
    }
    
    /**
     * Eliminar tag - NO PERMITIDO
     * Se lanza excepción indicando que solo se puede renombrar
     */
    public void delete(Long id, Long ownerId) {
        throw new UnsupportedOperationException(
            "No se pueden eliminar tags. Si necesitas cambiar el nombre, usa la opción de editar."
        );
    }
    
    /**
     * Buscar tag por nombre (para detectar conflictos en importación)
     */
    public Tag findByNameAndOwnerId(String name, Long ownerId) {
        return tagRepository.findByNameAndOwnerId(name, ownerId).orElse(null);
    }
    
    /**
     * Conversión de entidad a DTO
     */
    private TagDTO convertToDTO(Tag tag) {
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setColor(tag.getColor());
        dto.setOwnerId(tag.getOwnerId());
        dto.setCreatedAt(tag.getCreatedAt());
        return dto;
    }
}
