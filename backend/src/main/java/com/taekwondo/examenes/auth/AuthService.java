package com.taekwondo.examenes.auth;

import com.taekwondo.examenes.club.Club;
import com.taekwondo.examenes.club.ClubRepository;
import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import com.taekwondo.examenes.security.JwtService;
import com.taekwondo.examenes.user.User;
import com.taekwondo.examenes.user.UserRepository;
import com.taekwondo.examenes.user.UserResponse;
import com.taekwondo.examenes.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginThrottle loginThrottle;

    public AuthService(UserRepository userRepository, ClubRepository clubRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, LoginThrottle loginThrottle) {
        this.userRepository = userRepository;
        this.clubRepository = clubRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginThrottle = loginThrottle;
    }

    /** Registro público: siempre crea un STUDENT, con email y club opcionales (RF-01, RF-41). */
    public UserResponse register(RegisterRequest request) {
        // El club se comprueba antes de crear el usuario: con un club que no existe no se crea nada
        Club club = request.clubId() == null ? null : clubRepository.findById(request.clubId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe un club con id " + request.clubId()));
        User user = createUser(request.username(), request.email(), request.plainPassword(),
                request.displayName(), UserRole.STUDENT);
        if (club != null) {
            user.joinClub(club);
        }
        return UserResponse.from(user);
    }

    /** Alta de un ADMIN (solo desde el seed inicial). */
    public UserResponse createAdmin(String username, String email, String plainPassword, String displayName) {
        return UserResponse.from(createUser(username, email, plainPassword, displayName, UserRole.ADMIN));
    }

    /** Inicio de sesión con el límite de fallos por dirección y por cuenta (RNF-07). */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, String clientAddress) {
        String login = request.usernameOrEmail().trim();
        Optional<User> found = userRepository.findByUsername(login)
                .or(() -> userRepository.findByEmail(login.toLowerCase()));
        // Por usuario o por email, la misma cuenta; si no existe, lo que se escribió
        String account = found.map(u -> "u:" + u.getId()).orElse("l:" + login.toLowerCase());
        loginThrottle.checkAllowed(account, clientAddress);

        Optional<User> user = found
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(request.plainPassword(), u.getPasswordHash()));
        if (user.isEmpty()) {
            loginThrottle.recordFailure(account, clientAddress);
            throw new InvalidCredentialsException();
        }
        loginThrottle.recordSuccess(account);
        return session(user.get());
    }

    /**
     * Cambiar la propia contraseña (RF-06), también la temporal (RF-33). Cierra las demás sesiones
     * y devuelve una nueva para seguir en este dispositivo.
     */
    public LoginResponse changeOwnPassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("La contraseña actual no es correcta");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new BusinessRuleException("La nueva contraseña tiene que ser distinta de la actual");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        return session(user);
    }

    private LoginResponse session(User user) {
        return new LoginResponse(jwtService.generateAccessToken(user.getId(), user.getCredentialsVersion()),
                UserResponse.from(user));
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
        // Sin email (ausente o vacío) se guarda null: varios usuarios pueden no tenerlo
        String cleanEmail = email == null || email.isBlank() ? null : email.trim().toLowerCase();
        if (userRepository.existsByUsername(cleanUsername)) {
            throw new BusinessRuleException("Ya existe un usuario con username '" + cleanUsername + "'");
        }
        if (cleanEmail != null && userRepository.existsByEmail(cleanEmail)) {
            throw new BusinessRuleException("Ya existe un usuario con email '" + cleanEmail + "'");
        }
        User user = new User(cleanUsername, cleanEmail, passwordEncoder.encode(plainPassword),
                displayName.trim(), role);
        return userRepository.save(user);
    }
}
