package com.taekwondo.examenes.user;

import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("createdAt")).stream()
                .map(UserResponse::from)
                .toList();
    }

    /** Solo el administrador cambia roles (RF-36); ver SecurityConfig. */
    public UserResponse changeRole(Long userId, UserRole newRole, Long requesterId) {
        if (userId.equals(requesterId)) {
            // Evita que el último admin se quite el rol a sí mismo y nadie pueda gestionar la app
            throw new BusinessRuleException("No puedes cambiar tu propio rol");
        }
        User user = getUser(userId);
        user.changeRole(newRole);
        return UserResponse.from(user);
    }

    /**
     * El administrador pone una contraseña temporal (RF-35), tenga o no email el usuario.
     * Sus sesiones se cierran y tendrá que cambiarla al entrar (RF-33).
     */
    public UserResponse resetPassword(Long userId, String temporaryPassword, Long requesterId) {
        if (userId.equals(requesterId)) {
            // Se quedaría con una contraseña temporal y sin poder usar la app hasta cambiarla
            throw new BusinessRuleException("Para tu propia contraseña usa «Cambiar contraseña»");
        }
        User user = getUser(userId);
        user.resetPassword(passwordEncoder.encode(temporaryPassword));
        return UserResponse.from(user);
    }

    /** Desactivar o reactivar una cuenta (RF-05); la propia no, para no quedarse fuera. */
    public UserResponse setActive(Long userId, boolean active, Long requesterId) {
        if (userId.equals(requesterId)) {
            throw new BusinessRuleException("No puedes desactivar tu propia cuenta");
        }
        User user = getUser(userId);
        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }
        return UserResponse.from(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + userId));
    }
}
