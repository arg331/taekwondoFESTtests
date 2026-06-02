package com.taekwondo.examenes.infrastructure.exam;

import com.taekwondo.examenes.domain.port.ExamCodeGenerator;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Implementación de ExamCodeGenerator basada en UUID truncado.
 *
 * Formato: "EXM-XXXXXXXX" (8 caracteres hexadecimales en mayúsculas).
 * Espacio: 16^8 = ~4.300 millones de códigos.
 *
 * La unicidad NO se garantiza aquí (lo dice el contrato del puerto):
 * el caso de uso PublishExamUseCase comprueba si existe el código y,
 * en caso de colisión, vuelve a llamar a generate().
 *
 * UUID v4 es aleatorio criptográficamente seguro, así que tomar 8 chars
 * de él es válido para este propósito.
 */
@Component
public class UuidExamCodeGeneratorAdapter implements ExamCodeGenerator {

    private static final String PREFIX = "EXM-";
    private static final int HEX_LENGTH = 8;

    @Override
    public String generate() {
        String hex = UUID.randomUUID().toString().replace("-", "");
        String suffix = hex.substring(0, HEX_LENGTH).toUpperCase(Locale.ROOT);
        return PREFIX + suffix;
    }
}
