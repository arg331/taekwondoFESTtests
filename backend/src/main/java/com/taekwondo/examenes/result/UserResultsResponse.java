package com.taekwondo.examenes.result;

import java.util.List;

/** Un usuario y sus entregas (RF-55), la más reciente primero. clubName es null si no tiene club. */
public record UserResultsResponse(
        Long userId,
        String username,
        String displayName,
        String clubName,
        List<ResultResponse> results
) {}
