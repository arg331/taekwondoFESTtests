package com.taekwondo.examenes.infrastructure.config;

import com.taekwondo.examenes.application.auth.CreateUserUseCase;
import com.taekwondo.examenes.application.auth.GetCurrentUserUseCase;
import com.taekwondo.examenes.application.auth.LoginUseCase;
import com.taekwondo.examenes.application.auth.RegisterStudentUseCase;
import com.taekwondo.examenes.application.auth.ListUsersUseCase;
import com.taekwondo.examenes.application.auth.PromoteUserUseCase;
import com.taekwondo.examenes.application.exam.*;
import com.taekwondo.examenes.application.favorite.*;
import com.taekwondo.examenes.application.question.*;
import com.taekwondo.examenes.application.result.*;
import com.taekwondo.examenes.application.tag.*;
import com.taekwondo.examenes.domain.port.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BeanConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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

    @Bean
    public CreateUserUseCase createUserUseCase(UserRepository userRepository,
                                                PasswordHasher passwordHasher) {
        return new CreateUserUseCase(userRepository, passwordHasher);
    }

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
    public GetExamQuestionsByCodeUseCase getExamQuestionsByCodeUseCase(
            ExamRepository examRepository,
            QuestionRepository questionRepository,
            Clock clock) {
        return new GetExamQuestionsByCodeUseCase(examRepository, questionRepository, clock);
    }

    @Bean
    public ListMyExamsUseCase listMyExamsUseCase(ExamRepository examRepository) {
        return new ListMyExamsUseCase(examRepository);
    }

    @Bean
    public ListPublicExamsUseCase listPublicExamsUseCase(ExamRepository examRepository) {
        return new ListPublicExamsUseCase(examRepository);
    }

    @Bean
    public SubmitExamUseCase submitExamUseCase(ExamRepository examRepository,
                                                QuestionRepository questionRepository,
                                                ResultRepository resultRepository,
                                                Clock clock) {
        return new SubmitExamUseCase(examRepository, questionRepository, resultRepository, clock);
    }

    @Bean
    public GetResultUseCase getResultUseCase(ResultRepository resultRepository,
                                              ExamRepository examRepository) {
        return new GetResultUseCase(resultRepository, examRepository);
    }

    @Bean
    public ListResultsByExamUseCase listResultsByExamUseCase(ResultRepository resultRepository,
                                                              ExamRepository examRepository) {
        return new ListResultsByExamUseCase(resultRepository, examRepository);
    }

    @Bean
    public GetExamStatisticsUseCase getExamStatisticsUseCase(ResultRepository resultRepository,
                                                              ExamRepository examRepository) {
        return new GetExamStatisticsUseCase(resultRepository, examRepository);
    }

    @Bean
    public ListMyAttemptsUseCase listMyAttemptsUseCase(ResultRepository resultRepository) {
        return new ListMyAttemptsUseCase(resultRepository);
    }

    @Bean
    public FavoriteExamUseCase favoriteExamUseCase(ExamRepository examRepository,
                                                    ExamFavoriteRepository favoriteRepository) {
        return new FavoriteExamUseCase(examRepository, favoriteRepository);
    }

    @Bean
    public UnfavoriteExamUseCase unfavoriteExamUseCase(ExamFavoriteRepository favoriteRepository) {
        return new UnfavoriteExamUseCase(favoriteRepository);
    }

    @Bean
    public ListMyFavoriteExamsUseCase listMyFavoriteExamsUseCase(
            ExamFavoriteRepository favoriteRepository,
            ExamRepository examRepository) {
        return new ListMyFavoriteExamsUseCase(favoriteRepository, examRepository);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepository userRepository) {
        return new ListUsersUseCase(userRepository);
    }

    @Bean
    public PromoteUserUseCase promoteUserUseCase(UserRepository userRepository) {
        return new PromoteUserUseCase(userRepository);
    }
}
