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

    public UserResponse promote(Long userId) {
        User user = getUser(userId);
        user.promoteToAdmin();
        return UserResponse.from(user);
    }

    public UserResponse demote(Long userId, Long requesterId) {
        if (userId.equals(requesterId)) {
            // Evita que el último admin se quite el rol a sí mismo y nadie pueda gestionar la app
            throw new BusinessRuleException("No puedes quitarte el rol de ADMIN a ti mismo");
        }
        User user = getUser(userId);
        user.demoteToStudent();
        return UserResponse.from(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + userId));
    }
}
