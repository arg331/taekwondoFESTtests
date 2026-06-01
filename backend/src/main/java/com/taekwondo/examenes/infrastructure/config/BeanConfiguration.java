package com.taekwondo.examenes.infrastructure.config;

import com.taekwondo.examenes.application.auth.GetCurrentUserUseCase;
import com.taekwondo.examenes.application.auth.LoginUseCase;
import com.taekwondo.examenes.application.auth.RegisterStudentUseCase;
import com.taekwondo.examenes.application.question.CreateQuestionUseCase;
import com.taekwondo.examenes.application.question.DeleteQuestionUseCase;
import com.taekwondo.examenes.application.question.EditQuestionUseCase;
import com.taekwondo.examenes.application.question.GetQuestionUseCase;
import com.taekwondo.examenes.application.question.ListQuestionsUseCase;
import com.taekwondo.examenes.application.question.SearchQuestionsUseCase;
import com.taekwondo.examenes.application.tag.CreateTagUseCase;
import com.taekwondo.examenes.application.tag.GetTagUseCase;
import com.taekwondo.examenes.application.tag.ListTagsUseCase;
import com.taekwondo.examenes.application.tag.RenameTagUseCase;
import com.taekwondo.examenes.domain.port.JwtTokenProvider;
import com.taekwondo.examenes.domain.port.PasswordHasher;
import com.taekwondo.examenes.domain.port.QuestionRepository;
import com.taekwondo.examenes.domain.port.TagRepository;
import com.taekwondo.examenes.domain.port.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Wiring de Spring: convierte los casos de uso (clases Java puras
 * sin anotaciones) en beans de Spring.
 *
 * Esta clase es el ÚNICO punto donde Spring "ve" los casos de uso.
 *
 * Adapters de infraestructura (TagRepositoryJpaAdapter,
 * BCryptPasswordHasherAdapter, JwtTokenProviderAdapter, etc.) NO están
 * aquí: usan @Component / @Repository directamente.
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
    // CreateUserUseCase NO se registra: lo invocará el seed inicial, no HTTP.

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
}
