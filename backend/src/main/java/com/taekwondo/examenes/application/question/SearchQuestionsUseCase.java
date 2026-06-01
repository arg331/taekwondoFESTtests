package com.taekwondo.examenes.application.question;

import com.taekwondo.examenes.application.question.dto.QuestionView;
import com.taekwondo.examenes.application.question.dto.SearchQuestionsInput;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.domain.model.QuestionCriteria;
import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.domain.port.QuestionRepository;
import com.taekwondo.examenes.domain.port.TagRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Caso de uso: buscar preguntas de un profesor por criterios.
 *
 * Filtros disponibles (todos opcionales excepto ownerId):
 *  - anyOfTagIds: preguntas que contengan al menos uno de esos tags
 *  - textContains: el texto de la pregunta debe contener esta subcadena
 *
 * Si no se pasa ningún filtro, equivale a "listar todas".
 *
 * La traducción de tagIds (ids) a Tags (objetos) sigue el mismo patrón
 * que en CreateQuestionUseCase: validamos que existan y pertenezcan al
 * profesor. Esto evita fugas de información sobre tags ajenos.
 */
public class SearchQuestionsUseCase {

    private final QuestionRepository questionRepository;
    private final TagRepository tagRepository;

    public SearchQuestionsUseCase(QuestionRepository questionRepository,
                                   TagRepository tagRepository) {
        this.questionRepository = questionRepository;
        this.tagRepository = tagRepository;
    }

    public List<QuestionView> execute(SearchQuestionsInput input) {
        // 1. Resolver tags (si hay)
        Set<Tag> tags = resolveTagsForOwner(input.anyOfTagIds(), input.ownerId());

        // 2. Construir criterios
        QuestionCriteria criteria = QuestionCriteria.forOwner(input.ownerId())
                .withAnyOfTags(tags)
                .withTextContains(input.textContains())
                .build();

        // 3. Delegar al repositorio y convertir a vistas
        return questionRepository.findByCriteria(criteria).stream()
                .map(QuestionView::from)
                .toList();
    }

    private Set<Tag> resolveTagsForOwner(Set<Long> tagIds, Long ownerId) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Tag> resolved = new HashSet<>();
        for (Long tagId : tagIds) {
            Tag tag = tagRepository.findById(tagId)
                    .orElseThrow(() -> new BusinessRuleViolationException(
                            "El tag con id " + tagId + " no existe"));
            if (!tag.getOwnerId().equals(ownerId)) {
                throw new BusinessRuleViolationException(
                        "El tag con id " + tagId + " no existe");
            }
            resolved.add(tag);
        }
        return resolved;
    }
}
