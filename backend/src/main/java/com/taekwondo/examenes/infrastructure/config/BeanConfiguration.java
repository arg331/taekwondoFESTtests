package com.taekwondo.examenes.infrastructure.config;

import com.taekwondo.examenes.application.tag.CreateTagUseCase;
import com.taekwondo.examenes.application.tag.GetTagUseCase;
import com.taekwondo.examenes.application.tag.ListTagsUseCase;
import com.taekwondo.examenes.application.tag.RenameTagUseCase;
import com.taekwondo.examenes.domain.port.TagRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring de Spring: convierte los casos de uso (clases Java puras
 * sin anotaciones) en beans de Spring.
 *
 * Esta clase es el ÚNICO punto donde Spring "ve" los casos de uso.
 * Los casos de uso siguen sin anotaciones, manteniendo la pureza
 * de la capa de aplicación.
 *
 * Por qué este enfoque y no @Service en los casos de uso:
 *  - Mantiene la aplicación independiente del framework.
 *  - Tests unitarios sin Spring (instancias directas con mocks).
 *  - Si cambiamos de framework, solo cambia esta clase.
 *
 * A medida que añadamos casos de uso de otros agregados, los
 * registraremos aquí.
 */
@Configuration
public class BeanConfiguration {

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
}
