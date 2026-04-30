package com.taekwondo.examenes.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taekwondo.examenes.dto.ExamConfigDTO;
import com.taekwondo.examenes.dto.ExamDTO;
import com.taekwondo.examenes.dto.ExamSubmissionDTO;
import com.taekwondo.examenes.dto.QuestionDTO;
import com.taekwondo.examenes.dto.ResultDTO;
import com.taekwondo.examenes.exception.ResourceNotFoundException;
import com.taekwondo.examenes.model.Exam;
import com.taekwondo.examenes.model.Question;
import com.taekwondo.examenes.model.Result;
import com.taekwondo.examenes.repository.ExamRepository;
import com.taekwondo.examenes.repository.ResultRepository;

import lombok.RequiredArgsConstructor;

/**
 * Servicio para la lógica de negocio de Exam
 */
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final QuestionService questionService;
    private final ResultRepository resultRepository;

    /**
     * Crear nuevo examen con preguntas aleatorias
     */
    @Transactional
    public ExamDTO createExam(ExamConfigDTO config) {
        // Generar código único
        String code = generateUniqueCode();

        // Obtener preguntas aleatorias
        List<Question> randomQuestions = questionService.getRandomQuestions(config.getNumberOfQuestions());
        List<Long> questionIds = randomQuestions.stream()
                .map(Question::getId)
                .collect(Collectors.toList());

        // Crear examen
        Exam exam = new Exam();
        exam.setCode(code);
        exam.setNumberOfQuestions(config.getNumberOfQuestions());
        exam.setShowScore(config.getShowScore());
        exam.setTimeLimit(config.getTimeLimit());
        exam.setRandomizeOptions(config.getRandomizeOptions());
        exam.setQuestionIds(questionIds);
        exam.setActive(true);
        exam.setExpiresAt(LocalDateTime.now().plusDays(3)); // Expira en 3 días

        // Desactivar exámenes anteriores (solo uno activo a la vez)
        deactivatePreviousExams();

        Exam savedExam = examRepository.save(exam);
        return new ExamDTO(savedExam);
    }

    /**
     * Obtener examen por código (para que los estudiantes lo hagan)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getExamByCode(String code) {
        Exam exam = examRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Examen no encontrado con código: " + code));

        // Verificar que esté activo y no haya expirado
        if (!exam.getActive() || exam.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("El examen ha expirado o no está activo");
        }

        // Cargar las preguntas
        List<QuestionDTO> questions = exam.getQuestionIds().stream()
                .map(questionService::getQuestionById)
                .collect(Collectors.toList());

        // Si debe aleatorizar opciones, hacerlo aquí
        if (exam.getRandomizeOptions()) {
            questions = randomizeQuestionOptions(questions);
        }

        // Preparar respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("examId", exam.getId());
        response.put("code", exam.getCode());
        response.put("numberOfQuestions", exam.getNumberOfQuestions());
        response.put("timeLimit", exam.getTimeLimit());
        response.put("showScore", exam.getShowScore());
        response.put("questions", questions);

        return response;
    }

    /**
     * Enviar respuestas del examen y calcular resultado
     */
    @Transactional
    public ResultDTO submitExam(Long examId, ExamSubmissionDTO submission) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examen no encontrado con ID: " + examId));

        // Verificar que el estudiante no haya hecho ya este examen
        if (resultRepository.existsByStudentNameAndExamId(submission.getStudentName(), examId)) {
            throw new IllegalStateException("Este estudiante ya realizó este examen");
        }

        // Calcular resultados
        List<Result.Answer> answers = new ArrayList<>();
        int correctAnswers = 0;

        for (ExamSubmissionDTO.AnswerDTO answerDTO : submission.getAnswers()) {
            QuestionDTO question = questionService.getQuestionById(answerDTO.getQuestionId());
            
            boolean isCorrect = answerDTO.getStudentAnswer().equals(question.getCorrectAnswer());
            if (isCorrect) {
                correctAnswers++;
            }

            Result.Answer answer = new Result.Answer();
            answer.setQuestionId(answerDTO.getQuestionId());
            answer.setStudentAnswer(answerDTO.getStudentAnswer());
            answer.setCorrectAnswer(question.getCorrectAnswer());
            answer.setIsCorrect(isCorrect);
            
            answers.add(answer);
        }

        // Calcular puntuación (porcentaje)
        int totalQuestions = submission.getAnswers().size();
        int score = (int) Math.round((correctAnswers * 100.0) / totalQuestions);

        // Crear resultado
        Result result = new Result();
        result.setExam(exam);
        result.setStudentName(submission.getStudentName());
        result.setStudentClub(submission.getStudentClub());
        result.setStudentEmail(submission.getStudentEmail());
        result.setScore(score);
        result.setCorrectAnswers(correctAnswers);
        result.setTotalQuestions(totalQuestions);
        result.setTimeSpent(submission.getTimeSpent());
        result.setAnswers(answers);

        // Establecer la relación bidireccional
        for (Result.Answer answer : answers) {
            answer.setResult(result);
        }

        Result savedResult = resultRepository.save(result);
        return new ResultDTO(savedResult);
    }

    /**
     * Obtener examen activo actual
     */
    @Transactional(readOnly = true)
    public ExamDTO getActiveExam() {
        Exam exam = examRepository.findLatestActiveExam(LocalDateTime.now())
                .orElseThrow(() -> new ResourceNotFoundException("No hay examen activo en este momento"));
        return new ExamDTO(exam);
    }

    /**
     * Obtener todos los exámenes
     */
    @Transactional(readOnly = true)
    public List<ExamDTO> getAllExams() {
        return examRepository.findAll().stream()
                .map(ExamDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Generar código único para el examen
     */
    private String generateUniqueCode() {
        String code;
        do {
            code = "EXM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (examRepository.existsByCode(code));
        return code;
    }

    /**
     * Desactivar exámenes anteriores
     */
    private void deactivatePreviousExams() {
        List<Exam> activeExams = examRepository.findByActiveTrue();
        for (Exam exam : activeExams) {
            exam.setActive(false);
            examRepository.save(exam);
        }
    }

    /**
     * Aleatorizar opciones de las preguntas
     */
    private List<QuestionDTO> randomizeQuestionOptions(List<QuestionDTO> questions) {
        Random random = new Random();
        
        return questions.stream().map(question -> {
            List<String> options = new ArrayList<>(question.getOptions());
            String correctOption = options.get(question.getCorrectAnswer());
            
            // Mezclar opciones
            Collections.shuffle(options, random);
            
            // Encontrar nuevo índice de la respuesta correcta
            int newCorrectIndex = options.indexOf(correctOption);
            
           QuestionDTO randomizedQuestion = new QuestionDTO();
            randomizedQuestion.setId(question.getId());
            randomizedQuestion.setCategory(question.getCategory());
            randomizedQuestion.setDifficulty(question.getDifficulty());
            randomizedQuestion.setText(question.getText());
            randomizedQuestion.setOptions(options); // Las opciones ya mezcladas
            randomizedQuestion.setCorrectAnswer(newCorrectIndex);
            randomizedQuestion.setExplanation(question.getExplanation());
            randomizedQuestion.setCreatedAt(question.getCreatedAt());
            randomizedQuestion.setUpdatedAt(question.getUpdatedAt());
            randomizedQuestion.setCorrectAnswer(newCorrectIndex);
            
            return randomizedQuestion;
        }).collect(Collectors.toList());
    }
}
