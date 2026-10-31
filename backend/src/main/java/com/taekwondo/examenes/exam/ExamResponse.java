package com.taekwondo.examenes.exam;

import com.taekwondo.examenes.tag.TagResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record ExamResponse(
        Long id,
        String title,
        Long ownerId,
        ExamStatus status,
        Visibility visibility,
        ExamAccessMode accessMode,
        Config config,
        List<Long> questionIds,
        Set<TagResponse> generationTags,
        String code,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        /** Intentos por alumno; null = sin límite (RF-52). */
        Integer maxAttempts,
        /** Quién me lo ha compartido (RF-51): solo puedo consultarlo. null en los exámenes propios. */
        String sharedBy
) {
    public record Config(
            int numberOfQuestions,
            Integer timeLimitMinutes,
            boolean showScore,
            boolean showAnswers,
            boolean randomizeOptions,
            boolean randomizeQuestionOrder,
            /** % de aciertos para aprobar (DE-13). */
            int passingScore
    ) {
        static Config from(ExamConfig c) {
            return new Config(c.getNumberOfQuestions(), c.getTimeLimitMinutes(), c.isShowScore(), c.isShowAnswers(),
                    c.isRandomizeOptions(), c.isRandomizeQuestionOrder(), c.getPassingScore());
        }
    }

    public static ExamResponse from(Exam exam) {
        return from(exam, null);
    }

    /** Un examen que me ha compartido sharedBy (RF-51). */
    public static ExamResponse from(Exam exam, String sharedBy) {
        return new ExamResponse(exam.getId(), exam.getTitle(), exam.getOwnerId(), exam.getStatus(),
                exam.getVisibility(), exam.getAccessMode(), Config.from(exam.getConfig()),
                List.copyOf(exam.getQuestionIds()),
                exam.getGenerationTags().stream().map(TagResponse::from).collect(Collectors.toSet()),
                exam.getCode(), exam.getCreatedAt(), exam.getExpiresAt(), exam.getMaxAttempts(), sharedBy);
    }
}
