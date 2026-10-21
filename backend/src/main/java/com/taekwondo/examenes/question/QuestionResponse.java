package com.taekwondo.examenes.question;

import com.taekwondo.examenes.tag.TagResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record QuestionResponse(
        Long id,
        String text,
        List<String> options,
        /** Una o varias, de menor a mayor (RF-09). */
        List<Integer> correctAnswers,
        String explanation,
        Grade grade,
        QuestionType type,
        Set<TagResponse> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        /** Quién me la ha compartido (RF-57): solo puedo consultarla. null en las propias. */
        String sharedBy
) {
    public static QuestionResponse from(Question q) {
        return from(q, null);
    }

    /** Una pregunta de un examen que me ha compartido sharedBy (RF-57). */
    public static QuestionResponse from(Question q, String sharedBy) {
        return new QuestionResponse(q.getId(), q.getText(), List.copyOf(q.getOptions()),
                q.getCorrectAnswers(), q.getExplanation(), q.getGrade(), q.getType(),
                q.getTags().stream().map(TagResponse::from).collect(Collectors.toSet()),
                q.getCreatedAt(), q.getUpdatedAt(), sharedBy);
    }
}
