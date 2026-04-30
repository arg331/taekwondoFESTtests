package com.taekwondo.examenes.service;

import com.taekwondo.examenes.dto.QuestionDTO;
import com.taekwondo.examenes.exception.ResourceNotFoundException;
import com.taekwondo.examenes.model.Question;
import com.taekwondo.examenes.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la lógica de negocio de Question
 */
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    /**
     * Obtener todas las preguntas
     */
    @Transactional(readOnly = true)
    public List<QuestionDTO> getAllQuestions() {
        return questionRepository.findAll().stream()
                .map(QuestionDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener pregunta por ID
     */
    @Transactional(readOnly = true)
    public QuestionDTO getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada con ID: " + id));
        return new QuestionDTO(question);
    }

    /**
     * Obtener preguntas por categoría
     */
    @Transactional(readOnly = true)
    public List<QuestionDTO> getQuestionsByCategory(String category) {
        Question.Category cat = Question.Category.valueOf(category.toUpperCase());
        return questionRepository.findByCategory(cat).stream()
                .map(QuestionDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Crear nueva pregunta
     */
    @Transactional
    public QuestionDTO createQuestion(QuestionDTO questionDTO) {
        // Validar que tenga 4 opciones
        if (questionDTO.getOptions() == null || questionDTO.getOptions().size() != 4) {
            throw new IllegalArgumentException("Una pregunta debe tener exactamente 4 opciones");
        }

        // Validar que la respuesta correcta esté en rango 0-3
        if (questionDTO.getCorrectAnswer() < 0 || questionDTO.getCorrectAnswer() > 3) {
            throw new IllegalArgumentException("La respuesta correcta debe estar entre 0 y 3");
        }

        Question question = questionDTO.toEntity();
        Question savedQuestion = questionRepository.save(question);
        return new QuestionDTO(savedQuestion);
    }

    /**
     * Actualizar pregunta existente
     */
    @Transactional
    public QuestionDTO updateQuestion(Long id, QuestionDTO questionDTO) {
        Question existingQuestion = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada con ID: " + id));

        // Validaciones
        if (questionDTO.getOptions() == null || questionDTO.getOptions().size() != 4) {
            throw new IllegalArgumentException("Una pregunta debe tener exactamente 4 opciones");
        }

        if (questionDTO.getCorrectAnswer() < 0 || questionDTO.getCorrectAnswer() > 3) {
            throw new IllegalArgumentException("La respuesta correcta debe estar entre 0 y 3");
        }

        // Actualizar campos
        existingQuestion.setCategory(Question.Category.valueOf(questionDTO.getCategory()));
        existingQuestion.setDifficulty(Question.Difficulty.valueOf(questionDTO.getDifficulty()));
        existingQuestion.setText(questionDTO.getText());
        existingQuestion.setOptions(questionDTO.getOptions());
        existingQuestion.setCorrectAnswer(questionDTO.getCorrectAnswer());
        existingQuestion.setExplanation(questionDTO.getExplanation());

        Question updatedQuestion = questionRepository.save(existingQuestion);
        return new QuestionDTO(updatedQuestion);
    }

    /**
     * Eliminar pregunta
     */
    @Transactional
    public void deleteQuestion(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pregunta no encontrada con ID: " + id);
        }
        questionRepository.deleteById(id);
    }

    /**
     * Contar preguntas totales
     */
    @Transactional(readOnly = true)
    public long countQuestions() {
        return questionRepository.count();
    }

    /**
     * Contar preguntas por categoría
     */
    @Transactional(readOnly = true)
    public long countQuestionsByCategory(String category) {
        Question.Category cat = Question.Category.valueOf(category.toUpperCase());
        return questionRepository.countByCategory(cat);
    }

    /**
     * Buscar preguntas por texto
     */
    @Transactional(readOnly = true)
    public List<QuestionDTO> searchQuestions(String searchText) {
        return questionRepository.findByTextContainingIgnoreCase(searchText).stream()
                .map(QuestionDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener preguntas aleatorias (usado por ExamService)
     */
    @Transactional(readOnly = true)
    public List<Question> getRandomQuestions(int count) {
        long totalQuestions = questionRepository.count();
        
        if (totalQuestions < count) {
            throw new IllegalArgumentException(
                "No hay suficientes preguntas. Disponibles: " + totalQuestions + ", solicitadas: " + count
            );
        }

        return questionRepository.findRandomQuestions(count);
    }
}
