package com.taekwondo.examenes.characterization.support;

import com.jayway.jsonpath.JsonPath;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Respuesta HTTP capturada: código de estado y cuerpo, con lectura por JSONPath. */
public record HttpResult(int status, String body) {

    /** Comprueba el código de estado; si falla, el mensaje incluye el cuerpo. */
    public HttpResult expectStatus(int expected) {
        assertThat(status).as("HTTP %s, cuerpo: %s", status, body).isEqualTo(expected);
        return this;
    }

    public <T> T read(String path) {
        return JsonPath.read(body, path);
    }

    public long id() {
        return readLong("$.id");
    }

    public long readLong(String path) {
        return ((Number) read(path)).longValue();
    }

    public Map<String, Object> asMap() {
        return read("$");
    }

    public List<Map<String, Object>> asList() {
        return read("$");
    }

    /** Mensaje del cuerpo de error estándar { timestamp, status, error, message }. */
    public String message() {
        return read("$.message");
    }
}
