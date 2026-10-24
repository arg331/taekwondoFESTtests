package com.taekwondo.examenes.user;

import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import com.taekwondo.examenes.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

@Service
@Transactional
public class UserService {

    /** Más por página haría la lista tan pesada como sin paginar. */
    static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Una página de usuarios por fecha de alta, buscando por nombre, usuario o email (RF-37, RNF-09). */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("createdAt").and(Sort.by("id")));
        return PageResponse.from(userRepository.search(searchText(query), pageable), UserResponse::from);
    }

    /** En minúsculas, sin tildes y con los comodines de LIKE escapados. */
    private static String searchText(String query) {
        String plain = Normalizer.normalize(query == null ? "" : query.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return plain.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
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
