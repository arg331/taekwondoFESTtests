package com.taekwondo.examenes.service;

import com.taekwondo.examenes.dto.*;
import com.taekwondo.examenes.exception.ResourceNotFoundException;
import com.taekwondo.examenes.model.*;
import com.taekwondo.examenes.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de Exámenes
 * 
 * FLUJO NUEVO:
 * 1. createDraft() - Crea examen borrador con pre-generación de preguntas
 * 2. updateQuestions() - Ajusta preguntas en la vista previa
 * 3. publishExam() - Publica el examen (isDraft = false, genera QR)
 */
@Service
@RequiredArgsConstructor
public class ExamService {
    
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final TagRepository tagRepository;
    private final ExamFavoriteRepository favoriteRepository;
    private final ResultRepository resultRepository;
    
    /**
     * PASO 1: Crear draft de examen con pre-generación de preguntas
     */
    @Transactional
    public ExamDraftDTO createDraft(ExamConfigDTO config, Long ownerId) {
        // Validar configuración
        if (config.getNumberOfQuestions() < 5 || config.getNumberOfQuestions() > 50) {
            throw new IllegalArgumentException("El número de preguntas debe estar entre 5 y 50");
        }
        
        if (config.getTimeLimit() != null && (config.getTimeLimit() < 0 || config.getTimeLimit() > 180)) {
            throw new IllegalArgumentException("El tiempo límite debe estar entre 0 y 180 minutos");
        }
        
        // Obtener tags permitidos
        Set<Tag> allowedTags = new HashSet<>();
        if (config.getAllowedTagIds() != null && !config.getAllowedTagIds().isEmpty()) {
            allowedTags = config.getAllowedTagIds().stream()
                .map(tagId -> tagRepository.findById(tagId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tag no encontrado: " + tagId)))
                .collect(Collectors.toSet());
            
            // Verificar que todos los tags pertenecen al profesor
            for (Tag tag : allowedTags) {
                if (!tag.getOwnerId().equals(ownerId)) {
                    throw new IllegalArgumentException("Tag no pertenece al profesor: " + tag.getId());
                }
            }
        }
        
        // Pre-seleccionar preguntas aleatorias
        List<Long> tagIds = allowedTags.stream().map(Tag::getId).collect(Collectors.toList());
        List<Question> preSelectedQuestions;
        
        if (tagIds.isEmpty()) {
            // Si no hay tags, seleccionar de todas las preguntas del profesor
            List<Question> allQuestions = questionRepository.findByOwnerId(ownerId);
            Collections.shuffle(allQuestions);
            preSelectedQuestions = allQuestions.stream()
                .limit(config.getNumberOfQuestions())
                .collect(Collectors.toList());
        } else {
            preSelectedQuestions = questionRepository.findRandomByOwnerIdAndTags(
                ownerId,
                tagIds,
                config.getNumberOfQuestions()
            );
        }
        
        // Verificar que hay suficientes preguntas
        if (preSelectedQuestions.size() < config.getNumberOfQuestions()) {
            throw new IllegalArgumentException(
                "No hay suficientes preguntas con los tags seleccionados. " +
                "Se encontraron " + preSelectedQuestions.size() + " pero se necesitan " + config.getNumberOfQuestions()
            );
        }
        
        // Crear examen draft
        Exam exam = new Exam();
        exam.setTitle(config.getTitle());
        exam.setOwnerId(ownerId);
        exam.setIsDraft(true);
        exam.setIsPublic(false); // Por defecto privado
        exam.setAllowedTags(allowedTags);
        exam.setNumberOfQuestions(config.getNumberOfQuestions());
        exam.setShowScore(config.getShowScore() != null ? config.getShowScore() : true);
        exam.setTimeLimit(config.getTimeLimit());
        exam.setRandomizeOptions(config.getRandomizeOptions() != null ? config.getRandomizeOptions() : false);
        exam.setRandomizeQuestionOrder(config.getRandomizeQuestionOrder() != null ? config.getRandomizeQuestionOrder() : false);
        exam.setActive(false);
        exam.setCode(""); // Se genera al publicar
        
        // Guardar IDs de preguntas pre-seleccionadas
        List<Long> questionIds = preSelectedQuestions.stream()
            .map(Question::getId)
            .collect(Collectors.toList());
        exam.setQuestionIds(questionIds);
        
        Exam saved = examRepository.save(exam);
        
        // Convertir a DTO con preguntas completas
        return convertToDraftDTO(saved, preSelectedQuestions);
    }
    
    /**
     * PASO 2: Actualizar preguntas del draft (vista de ajuste)
     */
    @Transactional
    public void updateQuestions(Long examId, List<Long> questionIds, Long ownerId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new ResourceNotFoundException("Examen no encontrado: " + examId));
        
        // Verificar que pertenece al profesor
        if (!exam.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("Examen no encontrado");
        }
        
        // Verificar que está en draft
        if (!exam.getIsDraft()) {
            throw new IllegalArgumentException("No se puede modificar un examen publicado");
        }
        
        // Validar que no haya nulls (slots vacíos no están permitidos al guardar)
        // En el frontend pueden existir slots vacíos temporalmente, pero al guardar deben estar llenos
        if (questionIds.contains(null)) {
            throw new IllegalArgumentException("Hay preguntas sin asignar (slots vacíos)");
        }
        
        // Validar que el número de preguntas coincida
        if (questionIds.size() != exam.getNumberOfQuestions()) {
            throw new IllegalArgumentException(
                "Se esperaban " + exam.getNumberOfQuestions() + " preguntas pero se recibieron " + questionIds.size()
            );
        }
        
        // Actualizar
        exam.setQuestionIds(questionIds);
        examRepository.save(exam);
    }
    
    /**
     * PASO 3: Publicar examen (genera código, desactiva anteriores)
     */
    @Transactional
    public ExamPublishResponseDTO publishExam(Long examId, Long ownerId, Boolean isPublic) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new ResourceNotFoundException("Examen no encontrado: " + examId));
        
        // Verificar que pertenece al profesor
        if (!exam.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("Examen no encontrado");
        }
        
        // Verificar que está en draft
        if (!exam.getIsDraft()) {
            throw new IllegalArgumentException("Este examen ya está publicado");
        }
        
        // Validar que todos los slots estén llenos
        if (exam.getQuestionIds().size() != exam.getNumberOfQuestions()) {
            throw new IllegalArgumentException("El examen no está completo");
        }
        
        // Verificar que ninguna pregunta sea null
        if (exam.getQuestionIds().contains(null)) {
            throw new IllegalArgumentException("El examen tiene preguntas sin asignar");
        }
        
        // Desactivar exámenes anteriores del profesor
        List<Exam> activeExams = examRepository.findByOwnerIdAndActiveTrue(ownerId);
        for (Exam activeExam : activeExams) {
            activeExam.setActive(false);
            examRepository.save(activeExam);
        }
        
        // Generar código único
        String code = generateUniqueCode();
        
        // Publicar
        exam.setIsDraft(false);
        exam.setActive(true);
        exam.setCode(code);
        exam.setIsPublic(isPublic != null ? isPublic : false);
        exam.setExpiresAt(LocalDateTime.now().plusDays(3));
        
        Exam published = examRepository.save(exam);
        
        // Generar QR (simplificado - en producción usar ZXing)
        String qrCodeUrl = "data:image/png;base64,iVBORw0KGgo..."; // Placeholder
        String publicUrl = "https://dominio.com/exam/" + code;
        
        return new ExamPublishResponseDTO(
            published.getId(),
            code,
            qrCodeUrl,
            publicUrl,
            published.getExpiresAt()
        );
    }
    
    /**
     * Obtener draft de examen (para vista de ajuste)
     */
    public ExamDraftDTO getDraft(Long examId, Long ownerId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new ResourceNotFoundException("Examen no encontrado: " + examId));
        
        // Verificar que pertenece al profesor
        if (!exam.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("Examen no encontrado");
        }
        
        // Cargar preguntas
        List<Question> questions = exam.getQuestionIds().stream()
            .map(id -> questionRepository.findById(id).orElse(null))
            .collect(Collectors.toList());
        
        return convertToDraftDTO(exam, questions);
    }
    
    /**
     * Obtener preguntas disponibles para agregar al examen
     */
    public List<QuestionDTO> getAvailableQuestions(Long examId, Long ownerId, String search) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new ResourceNotFoundException("Examen no encontrado: " + examId));
        
        // Verificar que pertenece al profesor
        if (!exam.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("Examen no encontrado");
        }
        
        // Obtener todas las preguntas con los tags permitidos
        List<Question> allQuestions;
        if (exam.getAllowedTags().isEmpty()) {
            allQuestions = questionRepository.findByOwnerId(ownerId);
        } else {
            List<Tag> tags = new ArrayList<>(exam.getAllowedTags());
            allQuestions = questionRepository.findByOwnerIdAndTagsIn(ownerId, tags);
        }
        
        // Filtrar las que ya están en el examen
        Set<Long> usedIds = new HashSet<>(exam.getQuestionIds());
        List<Question> available = allQuestions.stream()
            .filter(q -> !usedIds.contains(q.getId()))
            .collect(Collectors.toList());
        
        // Aplicar búsqueda si se especifica
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            available = available.stream()
                .filter(q -> q.getText().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
        }
        
        return available.stream()
            .map(this::convertQuestionToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Exportar examen a CSV
     */
    public String exportToCSV(Long examId, Long ownerId) throws IOException {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new ResourceNotFoundException("Examen no encontrado: " + examId));
        
        // Verificar que pertenece al profesor o es favorito
        boolean isFavorite = favoriteRepository.existsByProfessorIdAndExamId(ownerId, examId);
        if (!exam.getOwnerId().equals(ownerId) && !isFavorite) {
            throw new ResourceNotFoundException("Examen no encontrado");
        }
        
        StringBuilder csv = new StringBuilder();
        
        // Metadatos del examen
        csv.append("# Metadatos del Examen\n");
        csv.append("exam_title,exam_time_limit,exam_show_score,exam_randomize_options,exam_randomize_order\n");
        csv.append(String.format("\"%s\",%d,%b,%b,%b\n",
            exam.getTitle(),
            exam.getTimeLimit() != null ? exam.getTimeLimit() : 0,
            exam.getShowScore(),
            exam.getRandomizeOptions(),
            exam.getRandomizeQuestionOrder()
        ));
        csv.append("\n");
        
        // Tags
        csv.append("# Tags\n");
        csv.append("tag_id,tag_name,tag_color\n");
        Map<Long, Integer> tagMapping = new HashMap<>();
        int tagCounter = 1;
        for (Tag tag : exam.getAllowedTags()) {
            csv.append(String.format("%d,\"%s\",\"%s\"\n",
                tagCounter,
                tag.getName(),
                tag.getColor()
            ));
            tagMapping.put(tag.getId(), tagCounter);
            tagCounter++;
        }
        csv.append("\n");
        
        // Preguntas
        csv.append("# Preguntas\n");
        csv.append("question_id,text,option1,option2,option3,option4,correct_answer,explanation,difficulty,tag_ids\n");
        int questionCounter = 1;
        for (Long questionId : exam.getQuestionIds()) {
            Question q = questionRepository.findById(questionId).orElse(null);
            if (q == null) continue;
            
            String tagIds = q.getTags().stream()
                .map(tag -> tagMapping.get(tag.getId()))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
            
            csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",%s,\"%s\"\n",
                questionCounter,
                escapeCsv(q.getText()),
                escapeCsv(q.getOptions().get(0)),
                escapeCsv(q.getOptions().get(1)),
                escapeCsv(q.getOptions().get(2)),
                escapeCsv(q.getOptions().get(3)),
                q.getCorrectAnswer(),
                escapeCsv(q.getExplanation() != null ? q.getExplanation() : ""),
                q.getDifficulty().name(),
                tagIds
            ));
            questionCounter++;
        }
        
        return csv.toString();
    }
    
    /**
     * Importar examen desde CSV
     * 
     * @param mergeStrategy "MERGE" = fusionar tags coincidentes, "NO_TAGS" = importar sin tags
     */
    @Transactional
    public ExamImportResponseDTO importFromCSV(MultipartFile file, Long ownerId, String mergeStrategy) throws IOException {
        // TODO: Parsear CSV completo
        // Esta es una versión simplificada
        
        throw new UnsupportedOperationException("Función de importación CSV en desarrollo");
    }
    
    /**
     * Listar exámenes según scope
     */
    public List<ExamDTO> getExams(String scope, Long ownerId) {
        List<Exam> exams;
        
        switch (scope.toLowerCase()) {
            case "my":
                // Mis exámenes (públicos y privados)
                exams = examRepository.findByOwnerId(ownerId);
                break;
                
            case "public":
                // Todos los exámenes públicos (de otros profesores)
                exams = examRepository.findByIsPublicTrueAndOwnerIdNot(ownerId);
                break;
                
            case "favorites":
                // Mis favoritos
                List<Long> favoriteIds = favoriteRepository.findExamIdsByProfessorId(ownerId);
                exams = examRepository.findAllById(favoriteIds);
                break;
                
            default:
                throw new IllegalArgumentException("Scope inválido: " + scope);
        }
        
        return exams.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    private String generateUniqueCode() {
        String code;
        do {
            code = "EXM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (examRepository.existsByCode(code));
        return code;
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
    
    private ExamDraftDTO convertToDraftDTO(Exam exam, List<Question> questions) {
        ExamDraftDTO dto = new ExamDraftDTO();
        dto.setId(exam.getId());
        dto.setTitle(exam.getTitle());
        dto.setIsDraft(exam.getIsDraft());
        dto.setNumberOfQuestions(exam.getNumberOfQuestions());
        dto.setAllowedTags(exam.getAllowedTags());
        dto.setTimeLimit(exam.getTimeLimit());
        dto.setShowScore(exam.getShowScore());
        dto.setRandomizeOptions(exam.getRandomizeOptions());
        dto.setRandomizeQuestionOrder(exam.getRandomizeQuestionOrder());
        dto.setQuestions(questions.stream()
            .map(this::convertQuestionToDTO)
            .collect(Collectors.toList()));
        return dto;
    }
    
    private ExamDTO convertToDTO(Exam exam) {
        ExamDTO dto = new ExamDTO();
        dto.setId(exam.getId());
        dto.setCode(exam.getCode());
        dto.setTitle(exam.getTitle());
        dto.setOwnerId(exam.getOwnerId());
        dto.setIsDraft(exam.getIsDraft());
        dto.setIsPublic(exam.getIsPublic());
        dto.setNumberOfQuestions(exam.getNumberOfQuestions());
        dto.setShowScore(exam.getShowScore());
        dto.setTimeLimit(exam.getTimeLimit());
        dto.setActive(exam.getActive());
        dto.setCreatedAt(exam.getCreatedAt());
        dto.setExpiresAt(exam.getExpiresAt());
        return dto;
    }
    
    private QuestionDTO convertQuestionToDTO(Question q) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(q.getId());
        dto.setText(q.getText());
        dto.setOptions(q.getOptions());
        dto.setCorrectAnswer(q.getCorrectAnswer());
        dto.setExplanation(q.getExplanation());
        dto.setDifficulty(q.getDifficulty().name());
        dto.setTags(q.getTags());
        return dto;
    }
}
