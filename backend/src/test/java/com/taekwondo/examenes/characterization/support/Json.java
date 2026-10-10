package com.taekwondo.examenes.characterization.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializador JSON mínimo para construir cuerpos de petición en los tests sin
 * depender de las clases de producción ni de la versión de Jackson.
 */
public final class Json {

    private Json() {}

    /** Objeto JSON a partir de pares clave/valor; admite valores null. */
    public static Map<String, Object> obj(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Se esperan pares clave/valor");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    public static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            map.forEach((k, v) -> sb.append(sb.length() > 1 ? "," : "")
                    .append(write(k.toString())).append(":").append(write(v)));
            return sb.append("}").toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            list.forEach(v -> sb.append(sb.length() > 1 ? "," : "").append(write(v)));
            return sb.append("]").toString();
        }
        throw new IllegalArgumentException("Tipo no soportado: " + value.getClass());
    }
}
