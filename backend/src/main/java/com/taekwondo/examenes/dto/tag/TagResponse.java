package com.taekwondo.examenes.dto.tag;

import com.taekwondo.examenes.entity.Tag;

import java.time.LocalDateTime;

public record TagResponse(Long id, String name, String color, LocalDateTime createdAt) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getColor(), tag.getCreatedAt());
    }
}
