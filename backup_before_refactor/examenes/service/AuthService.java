package com.taekwondo.examenes.service;

import com.taekwondo.examenes.dto.JwtResponse;
import com.taekwondo.examenes.dto.LoginRequest;
import com.taekwondo.examenes.exception.ResourceNotFoundException;
import com.taekwondo.examenes.model.User;
import com.taekwondo.examenes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la lógica de autenticación
 * 
 * NOTA: Este es un servicio temporal simplificado
 * En producción, usaremos JWT y Spring Security completo
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Autenticar usuario (versión simplificada)
     */
    @Transactional(readOnly = true)
    public JwtResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Verificar que esté activo
        if (!user.getActive()) {
            throw new IllegalStateException("Usuario desactivado");
        }

        // Verificar contraseña
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Contraseña incorrecta");
        }

        // Generar token (temporal - lo implementaremos con JWT después)
        String token = "temp-token-" + user.getUsername();

        return new JwtResponse(
            token,
            user.getUsername(),
            user.getEmail(),
            user.getRole().name()
        );
    }

    /**
     * Crear usuario admin inicial (solo para desarrollo)
     */
    @Transactional
    public User createAdminUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("El usuario ya existe");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(User.Role.ADMIN);
        user.setActive(true);

        return userRepository.save(user);
    }

    /**
     * Verificar si existen usuarios en el sistema
     */
    @Transactional(readOnly = true)
    public boolean hasUsers() {
        return userRepository.count() > 0;
    }
}
