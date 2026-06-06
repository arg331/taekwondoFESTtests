package com.taekwondo.examenes.application.auth;

import com.taekwondo.examenes.application.auth.dto.UserView;
import com.taekwondo.examenes.domain.port.UserRepository;

import java.util.List;

/**
 * Caso de uso: listar todos los usuarios del sistema.
 * Solo accesible por ADMIN (controlado en SecurityConfig).
 */
public class ListUsersUseCase {

    private final UserRepository userRepository;

    public ListUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserView> execute() {
        return userRepository.findAll().stream()
                .map(UserView::from)
                .toList();
    }
}
