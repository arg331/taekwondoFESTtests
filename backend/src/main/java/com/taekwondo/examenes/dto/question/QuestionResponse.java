package com.taekwondo.examenes.dto.question;

import com.taekwondo.examenes.dto.tag.TagResponse;
import com.taekwondo.examenes.entity.Difficulty;
import com.taekwondo.examenes.entity.Question;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record QuestionResponse(
        Long id,
        String text,
        List<String> options,
        int correctAnswer,
        String explanation,
        Difficulty difficulty,
        Set<TagResponse> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QuestionResponse from(Question q) {
        return new QuestionResponse(q.getId(), q.getText(), List.copyOf(q.getOptions()),
                q.getCorrectAnswer(), q.getExplanation(), q.getDifficulty(),
                q.getTags().stream().map(TagResponse::from).collect(Collectors.toSet()),
                q.getCreatedAt(), q.getUpdatedAt());
    }
}
