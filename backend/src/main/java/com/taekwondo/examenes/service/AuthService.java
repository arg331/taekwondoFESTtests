package com.taekwondo.examenes.service;

import com.taekwondo.examenes.dto.auth.LoginRequest;
import com.taekwondo.examenes.dto.auth.LoginResponse;
import com.taekwondo.examenes.dto.auth.RegisterRequest;
import com.taekwondo.examenes.dto.auth.UserResponse;
import com.taekwondo.examenes.entity.User;
import com.taekwondo.examenes.entity.UserRole;
import com.taekwondo.examenes.exception.BusinessRuleException;
import com.taekwondo.examenes.exception.InvalidCredentialsException;
import com.taekwondo.examenes.repository.UserRepository;
import com.taekwondo.examenes.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Registro público: siempre crea un STUDENT. */
    public UserResponse register(RegisterRequest request) {
        User user = createUser(request.username(), request.email(), request.plainPassword(),
                request.displayName(), UserRole.STUDENT);
        return UserResponse.from(user);
    }

    /** Alta de un ADMIN (solo desde el seed inicial). */
    public UserResponse createAdmin(String username, String email, String plainPassword, String displayName) {
        return UserResponse.from(createUser(username, email, plainPassword, displayName, UserRole.ADMIN));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String login = request.usernameOrEmail().trim();
        User user = userRepository.findByUsername(login)
                .or(() -> userRepository.findByEmail(login.toLowerCase()))
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(request.plainPassword(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return new LoginResponse(jwtService.generateAccessToken(user.getId()), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private User createUser(String username, String email, String plainPassword,
                            String displayName, UserRole role) {
        String cleanUsername = username.trim();
        String cleanEmail = email.trim().toLowerCase();
        if (userRepository.existsByUsername(cleanUsername)) {
            throw new BusinessRuleException("Ya existe un usuario con username '" + cleanUsername + "'");
        }
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new BusinessRuleException("Ya existe un usuario con email '" + cleanEmail + "'");
        }
        User user = new User(cleanUsername, cleanEmail, passwordEncoder.encode(plainPassword),
                displayName.trim(), role);
        return userRepository.save(user);
    }
}
