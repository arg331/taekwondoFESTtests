package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.application.auth.dto.LoginOutput;

/**
 * DTO HTTP de salida tras un login exitoso.
 *
 * Devuelve el token JWT (a usar en Authorization: Bearer <token>)
 * y los datos del usuario, evitando un segundo /me tras el login.
 */
public record LoginResponse(
        String token,
        UserResponse user
) {
    public static LoginResponse from(LoginOutput output) {
        return new LoginResponse(
                output.token(),
                UserResponse.from(output.user())
        );
    }
}
