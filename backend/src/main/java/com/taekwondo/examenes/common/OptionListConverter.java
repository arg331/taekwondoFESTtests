package com.taekwondo.examenes.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Índices de opciones guardados como «0,2» en una columna; «» es ninguno (pregunta en blanco). */
@Converter
public class OptionListConverter implements AttributeConverter<List<Integer>, String> {

    @Override
    public String convertToDatabaseColumn(List<Integer> options) {
        return options == null ? "" : options.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    @Override
    public List<Integer> convertToEntityAttribute(String column) {
        if (column == null || column.isBlank()) return List.of();
        return Arrays.stream(column.split(",")).map(String::trim).map(Integer::valueOf).toList();
    }
}
