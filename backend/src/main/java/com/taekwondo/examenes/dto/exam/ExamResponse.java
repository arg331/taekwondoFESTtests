package com.taekwondo.examenes.dto.exam;

import com.taekwondo.examenes.dto.tag.TagResponse;
import com.taekwondo.examenes.entity.Exam;
import com.taekwondo.examenes.entity.ExamAccessMode;
import com.taekwondo.examenes.entity.ExamConfig;
import com.taekwondo.examenes.entity.ExamStatus;
import com.taekwondo.examenes.entity.Visibility;

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
        LocalDateTime expiresAt
) {
    public record Config(
            int numberOfQuestions,
            Integer timeLimitMinutes,
            boolean showScore,
            boolean randomizeOptions,
            boolean randomizeQuestionOrder
    ) {
        static Config from(ExamConfig c) {
            return new Config(c.getNumberOfQuestions(), c.getTimeLimitMinutes(), c.isShowScore(),
                    c.isRandomizeOptions(), c.isRandomizeQuestionOrder());
        }
    }

    public static ExamResponse from(Exam exam) {
        return new ExamResponse(exam.getId(), exam.getTitle(), exam.getOwnerId(), exam.getStatus(),
                exam.getVisibility(), exam.getAccessMode(), Config.from(exam.getConfig()),
                List.copyOf(exam.getQuestionIds()),
                exam.getGenerationTags().stream().map(TagResponse::from).collect(Collectors.toSet()),
                exam.getCode(), exam.getCreatedAt(), exam.getExpiresAt());
    }
}
