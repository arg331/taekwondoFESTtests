package com.taekwondo.examenes.tag;

import java.time.LocalDateTime;

public record TagResponse(Long id, String name, String color, LocalDateTime createdAt) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getColor(), tag.getCreatedAt());
    }
}
