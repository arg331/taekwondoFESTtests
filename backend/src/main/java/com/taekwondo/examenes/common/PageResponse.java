package com.taekwondo.examenes.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Una página de un listado (RNF-09): sus elementos, cuál es (desde 0), su tamaño y el total. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
