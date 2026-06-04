package com.taekwondo.examenes.infrastructure.web.controller;

import com.taekwondo.examenes.application.question.CreateQuestionUseCase;
import com.taekwondo.examenes.application.question.DeleteQuestionUseCase;
import com.taekwondo.examenes.application.question.EditQuestionUseCase;
import com.taekwondo.examenes.application.question.GetQuestionUseCase;
import com.taekwondo.examenes.application.question.ListQuestionsUseCase;
import com.taekwondo.examenes.application.question.SearchQuestionsUseCase;
import com.taekwondo.examenes.application.question.dto.CreateQuestionInput;
import com.taekwondo.examenes.application.question.dto.EditQuestionInput;
import com.taekwondo.examenes.application.question.dto.QuestionView;
import com.taekwondo.examenes.application.question.dto.SearchQuestionsInput;
import com.taekwondo.examenes.infrastructure.security.AuthenticatedUser;
import com.taekwondo.examenes.infrastructure.web.dto.CreateQuestionRequest;
import com.taekwondo.examenes.infrastructure.web.dto.EditQuestionRequest;
import com.taekwondo.examenes.infrastructure.web.dto.QuestionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Controller REST para Question.
 *
 * El ownerId se obtiene del usuario autenticado (JWT) vía AuthenticatedUser.
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final CreateQuestionUseCase createQuestionUseCase;
    private final EditQuestionUseCase editQuestionUseCase;
    private final DeleteQuestionUseCase deleteQuestionUseCase;
    private final GetQuestionUseCase getQuestionUseCase;
    private final ListQuestionsUseCase listQuestionsUseCase;
    private final SearchQuestionsUseCase searchQuestionsUseCase;

    public QuestionController(CreateQuestionUseCase createQuestionUseCase,
                               EditQuestionUseCase editQuestionUseCase,
                               DeleteQuestionUseCase deleteQuestionUseCase,
                               GetQuestionUseCase getQuestionUseCase,
                               ListQuestionsUseCase listQuestionsUseCase,
                               SearchQuestionsUseCase searchQuestionsUseCase) {
        this.createQuestionUseCase = createQuestionUseCase;
        this.editQuestionUseCase = editQuestionUseCase;
        this.deleteQuestionUseCase = deleteQuestionUseCase;
        this.getQuestionUseCase = getQuestionUseCase;
        this.listQuestionsUseCase = listQuestionsUseCase;
        this.searchQuestionsUseCase = searchQuestionsUseCase;
    }

    @PostMapping
    public ResponseEntity<QuestionResponse> create(@RequestBody CreateQuestionRequest request) {
        CreateQuestionInput input = new CreateQuestionInput(
                request.text(),
                request.options(),
                request.correctAnswer(),
                request.explanation(),
                request.difficulty(),
                request.tagIds(),
                AuthenticatedUser.currentUserId()
        );
        QuestionView view = createQuestionUseCase.execute(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(QuestionResponse.from(view));
    }

    @PutMapping("/{id}")
    public QuestionResponse edit(@PathVariable Long id,
                                  @RequestBody EditQuestionRequest request) {
        EditQuestionInput input = new EditQuestionInput(
                id,
                request.text(),
                request.options(),
                request.correctAnswer(),
                request.explanation(),
                request.difficulty(),
                request.tagIds(),
                AuthenticatedUser.currentUserId()
        );
        QuestionView view = editQuestionUseCase.execute(input);
        return QuestionResponse.from(view);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteQuestionUseCase.execute(id, AuthenticatedUser.currentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public QuestionResponse get(@PathVariable Long id) {
        QuestionView view = getQuestionUseCase.execute(id, AuthenticatedUser.currentUserId());
        return QuestionResponse.from(view);
    }

    @GetMapping
    public List<QuestionResponse> list() {
        return listQuestionsUseCase.execute(AuthenticatedUser.currentUserId()).stream()
                .map(QuestionResponse::from)
                .toList();
    }

    @GetMapping("/search")
    public List<QuestionResponse> search(
            @RequestParam(required = false) Set<Long> tagIds,
            @RequestParam(required = false) String textContains) {

        SearchQuestionsInput input = new SearchQuestionsInput(
                AuthenticatedUser.currentUserId(),
                tagIds,
                textContains
        );
        return searchQuestionsUseCase.execute(input).stream()
                .map(QuestionResponse::from)
                .toList();
    }
}
