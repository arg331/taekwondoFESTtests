package com.taekwondo.examenes.infrastructure.web.exception;

import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.InvalidCredentialsException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Manejador global de excepciones del sistema.
 *
 * Categorías:
 *  - ResourceNotFoundException       → 404 Not Found (recurso del dominio)
 *  - NoResourceFoundException        → 404 Not Found (URL inexistente)
 *  - BusinessRuleViolationException  → 409 Conflict
 *  - InvalidCredentialsException     → 401 Unauthorized
 *  - IllegalArgumentException        → 400 Bad Request (validaciones dominio)
 *  - IllegalStateException           → 409 Conflict (transiciones inválidas)
 *  - HttpMessageNotReadableException → 400 Bad Request (JSON malformado)
 *  - MethodArgumentNotValidException → 400 Bad Request (@Valid fallido)
 *  - Throwable                       → 500 Internal Server Error (fallback)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * URLs inexistentes (no encontradas por el dispatcher). Antes caían
     * al handler genérico devolviendo 500; ahora devuelven 404 limpio.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado: " + ex.getResourcePath());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleViolationException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * JSON malformado en el body (ej: comillas mal, campo faltante en record).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "JSON malformado o petición inválida");
    }

    /**
     * Cuando @Valid en un DTO falla. Preparado para cuando lo usemos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Datos de petición inválidos");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Throwable ex) {
        // Loguear y devolver mensaje genérico (no exponemos detalles internos)
        ex.printStackTrace();
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
        return ResponseEntity.status(status).body(body);
    }
}
