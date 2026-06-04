package com.taekwondo.examenes.application.auth;

import com.taekwondo.examenes.application.auth.dto.UserView;
import com.taekwondo.examenes.application.shared.exception.InvalidCredentialsException;
import com.taekwondo.examenes.domain.model.User;
import com.taekwondo.examenes.domain.port.JwtTokenProvider;
import com.taekwondo.examenes.domain.port.UserRepository;

/**
 * Caso de uso: dado un token, devuelve la info del usuario autenticado.
 *
 * Útil para el endpoint /me que llama el frontend al arrancar para
 * recuperar la sesión del usuario.
 *
 * Si cualquier paso falla (token inválido, usuario borrado o desactivado),
 * lanza InvalidCredentialsException: el cliente debe volver a hacer login.
 */
public class GetCurrentUserUseCase {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public GetCurrentUserUseCase(UserRepository userRepository,
                                   JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public UserView execute(String token) {
        Long userId = jwtTokenProvider.extractUserId(token)
                .orElseThrow(InvalidCredentialsException::new);

        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        return UserView.from(user);
    }
}
