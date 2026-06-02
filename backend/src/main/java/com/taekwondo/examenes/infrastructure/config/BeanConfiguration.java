package com.taekwondo.examenes.infrastructure.config;

import com.taekwondo.examenes.application.auth.GetCurrentUserUseCase;
import com.taekwondo.examenes.application.auth.LoginUseCase;
import com.taekwondo.examenes.application.auth.RegisterStudentUseCase;
import com.taekwondo.examenes.application.exam.*;
import com.taekwondo.examenes.application.question.*;
import com.taekwondo.examenes.application.tag.*;
import com.taekwondo.examenes.domain.port.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Wiring de Spring: convierte los casos de uso (clases Java puras
 * sin anotaciones) en beans de Spring.
 *
 * Esta clase es el ÚNICO punto donde Spring "ve" los casos de uso.
 * Los adapters de infraestructura (TagRepositoryJpaAdapter, etc.)
 * usan @Component/@Repository directamente.
 */
@Configuration
public class BeanConfiguration {

    // ───── Beans de seguridad ─────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ───── Tag ─────

    @Bean
    public CreateTagUseCase createTagUseCase(TagRepository tagRepository) {
        return new CreateTagUseCase(tagRepository);
    }

    @Bean
    public GetTagUseCase getTagUseCase(TagRepository tagRepository) {
        return new GetTagUseCase(tagRepository);
    }

    @Bean
    public ListTagsUseCase listTagsUseCase(TagRepository tagRepository) {
        return new ListTagsUseCase(tagRepository);
    }

    @Bean
    public RenameTagUseCase renameTagUseCase(TagRepository tagRepository) {
        return new RenameTagUseCase(tagRepository);
    }

    // ───── Question ─────

    @Bean
    public CreateQuestionUseCase createQuestionUseCase(QuestionRepository questionRepository,
                                                        TagRepository tagRepository) {
        return new CreateQuestionUseCase(questionRepository, tagRepository);
    }

    @Bean
    public EditQuestionUseCase editQuestionUseCase(QuestionRepository questionRepository,
                                                    TagRepository tagRepository) {
        return new EditQuestionUseCase(questionRepository, tagRepository);
    }

    @Bean
    public DeleteQuestionUseCase deleteQuestionUseCase(QuestionRepository questionRepository) {
        return new DeleteQuestionUseCase(questionRepository);
    }

    @Bean
    public GetQuestionUseCase getQuestionUseCase(QuestionRepository questionRepository) {
        return new GetQuestionUseCase(questionRepository);
    }

    @Bean
    public ListQuestionsUseCase listQuestionsUseCase(QuestionRepository questionRepository) {
        return new ListQuestionsUseCase(questionRepository);
    }

    @Bean
    public SearchQuestionsUseCase searchQuestionsUseCase(QuestionRepository questionRepository,
                                                          TagRepository tagRepository) {
        return new SearchQuestionsUseCase(questionRepository, tagRepository);
    }

    // ───── Auth ─────

    @Bean
    public RegisterStudentUseCase registerStudentUseCase(UserRepository userRepository,
                                                          PasswordHasher passwordHasher) {
        return new RegisterStudentUseCase(userRepository, passwordHasher);
    }

    @Bean
    public LoginUseCase loginUseCase(UserRepository userRepository,
                                      PasswordHasher passwordHasher,
                                      JwtTokenProvider jwtTokenProvider) {
        return new LoginUseCase(userRepository, passwordHasher, jwtTokenProvider);
    }

    @Bean
    public GetCurrentUserUseCase getCurrentUserUseCase(UserRepository userRepository,
                                                        JwtTokenProvider jwtTokenProvider) {
        return new GetCurrentUserUseCase(userRepository, jwtTokenProvider);
    }

    // ───── Exam ─────

    @Bean
    public CreateExamDraftUseCase createExamDraftUseCase(ExamRepository examRepository) {
        return new CreateExamDraftUseCase(examRepository);
    }

    @Bean
    public PreGenerateExamDraftUseCase preGenerateExamDraftUseCase(
            ExamRepository examRepository,
            QuestionRepository questionRepository,
            TagRepository tagRepository) {
        return new PreGenerateExamDraftUseCase(examRepository, questionRepository, tagRepository);
    }

    @Bean
    public RenameExamUseCase renameExamUseCase(ExamRepository examRepository) {
        return new RenameExamUseCase(examRepository);
    }

    @Bean
    public ChangeExamConfigUseCase changeExamConfigUseCase(ExamRepository examRepository) {
        return new ChangeExamConfigUseCase(examRepository);
    }

    @Bean
    public UpdateExamQuestionsUseCase updateExamQuestionsUseCase(
            ExamRepository examRepository,
            QuestionRepository questionRepository) {
        return new UpdateExamQuestionsUseCase(examRepository, questionRepository);
    }

    @Bean
    public ChangeExamVisibilityUseCase changeExamVisibilityUseCase(ExamRepository examRepository) {
        return new ChangeExamVisibilityUseCase(examRepository);
    }

    @Bean
    public PublishExamUseCase publishExamUseCase(ExamRepository examRepository,
                                                  Clock clock,
                                                  ExamCodeGenerator codeGenerator) {
        return new PublishExamUseCase(examRepository, clock, codeGenerator);
    }

    @Bean
    public CloseExamUseCase closeExamUseCase(ExamRepository examRepository) {
        return new CloseExamUseCase(examRepository);
    }

    @Bean
    public ReopenExamUseCase reopenExamUseCase(ExamRepository examRepository, Clock clock) {
        return new ReopenExamUseCase(examRepository, clock);
    }

    @Bean
    public ExtendExamExpirationUseCase extendExamExpirationUseCase(ExamRepository examRepository,
                                                                    Clock clock) {
        return new ExtendExamExpirationUseCase(examRepository, clock);
    }

    @Bean
    public DeleteExamDraftUseCase deleteExamDraftUseCase(ExamRepository examRepository) {
        return new DeleteExamDraftUseCase(examRepository);
    }

    @Bean
    public GetExamUseCase getExamUseCase(ExamRepository examRepository) {
        return new GetExamUseCase(examRepository);
    }

    @Bean
    public GetExamByCodeUseCase getExamByCodeUseCase(ExamRepository examRepository, Clock clock) {
        return new GetExamByCodeUseCase(examRepository, clock);
    }

    @Bean
    public ListMyExamsUseCase listMyExamsUseCase(ExamRepository examRepository) {
        return new ListMyExamsUseCase(examRepository);
    }

    @Bean
    public ListPublicExamsUseCase listPublicExamsUseCase(ExamRepository examRepository) {
        return new ListPublicExamsUseCase(examRepository);
    }
}
