package com.taekwondo.examenes.application.exam;

import java.time.LocalDateTime;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.application.exam.dto.PublishExamInput;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.port.Clock;
import com.taekwondo.examenes.domain.port.ExamCodeGenerator;
import com.taekwondo.examenes.domain.port.ExamRepository;

/**
 * Caso de uso: publicar un draft de examen.
 *
 * Responsabilidades:
 *  1. Verificar que el examen existe y pertenece al solicitante.
 *  2. Determinar la fecha de expiración (default: +1 hora desde ahora).
 *  3. Validar que la fecha de expiración sea futura.
 *  4. Generar un código único (con verificación de colisión contra el repo).
 *  5. Pasar el examen a estado PUBLISHED (la entidad valida el resto).
 *
 * Aquí es donde Clock y ExamCodeGenerator hacen su trabajo:
 *  - Clock: para saber "ahora" sin acoplarnos a LocalDateTime.now().
 *  - ExamCodeGenerator: para no acoplar la lógica a una estrategia concreta.
 */
public class PublishExamUseCase {

    private static final long DEFAULT_EXPIRATION_HOURS = 1L;
    private static final int MAX_CODE_GENERATION_ATTEMPTS = 5;

    private final ExamRepository examRepository;
    private final Clock clock;
    private final ExamCodeGenerator codeGenerator;

    public PublishExamUseCase(ExamRepository examRepository,
                               Clock clock,
                               ExamCodeGenerator codeGenerator) {
        this.examRepository = examRepository;
        this.clock = clock;
        this.codeGenerator = codeGenerator;
    }

    public ExamView execute(PublishExamInput input) {
        // 1. Cargar el examen y comprobar pertenencia
        Exam exam = examRepository.findById(input.examId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + input.examId()));

        if (!exam.getOwnerId().equals(input.requesterOwnerId())) {
            throw new ResourceNotFoundException(
                    "No existe un examen con id " + input.examId());
        }

        // 2. Determinar fecha de expiración:
        //    - Si el cliente envía una, la usamos.
        //    - Si no, aplicamos el default (+1 hora desde ahora).
        LocalDateTime now = clock.now();
        LocalDateTime expiresAt = (input.expiresAt() != null)
                ? input.expiresAt()
                : now.plusHours(DEFAULT_EXPIRATION_HOURS);

        // 3. Validar que la fecha sea futura.
        //    (La entidad no puede validar esto: no tiene acceso al Clock.
        //    Por eso lo hacemos aquí.)
        if (!expiresAt.isAfter(now)) {
            throw new BusinessRuleViolationException(
                    "La fecha de expiración debe ser posterior al momento de publicación");
        }

        // 4. Generar un código único.
        //    ExamCodeGenerator no garantiza unicidad absoluta entre llamadas
        //    (ver Javadoc del puerto), así que reintentamos en caso de colisión.
        String code = generateUniqueCode();

        // 5. Publicar (la entidad valida estado DRAFT, código no vacío,
        //    visibilidad no null, número correcto de preguntas).
        exam.publish(code, input.visibility(), expiresAt);

        Exam saved = examRepository.save(exam);
        return ExamView.from(saved);
    }

    /**
     * Genera un código que no choca con ningún examen existente.
     *
     * Hace reintentos limitados: con un generador razonable (UUID, hashes...)
     * la probabilidad de colisión es muy baja. Si tras MAX_ATTEMPTS no lo
     * consigue, falla explícitamente para no entrar en bucle infinito.
     */
    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (!examRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException(
                "No se pudo generar un código único tras "
                        + MAX_CODE_GENERATION_ATTEMPTS + " intentos");
    }
}