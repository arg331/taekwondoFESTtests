package com.taekwondo.examenes.application.auth;

import com.taekwondo.examenes.application.auth.dto.UserView;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.User;
import com.taekwondo.examenes.domain.port.UserRepository;

public class PromoteUserUseCase {

    private final UserRepository userRepository;

    public PromoteUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserView promote(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id " + userId));
        try {
            user.promoteToAdmin();
        } catch (IllegalStateException e) {
            throw new BusinessRuleViolationException(e.getMessage());
        }
        return UserView.from(userRepository.save(user));
    }

    public UserView demote(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id " + userId));
        try {
            user.demoteToStudent();
        } catch (IllegalStateException e) {
            throw new BusinessRuleViolationException(e.getMessage());
        }
        return UserView.from(userRepository.save(user));
    }
}
