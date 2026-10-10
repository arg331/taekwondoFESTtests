package com.taekwondo.examenes.user;

import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + userId));
    }
}
