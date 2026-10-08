package com.taekwondo.examenes.config;

import com.taekwondo.examenes.entity.UserRole;
import com.taekwondo.examenes.repository.UserRepository;
import com.taekwondo.examenes.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea el primer ADMIN al arrancar si todavía no hay ninguno.
 *
 * En producción, definir antes de arrancar ADMIN_USERNAME, ADMIN_EMAIL,
 * ADMIN_PASSWORD y ADMIN_DISPLAY_NAME.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    private static final String DEFAULT_PASSWORD = "admin123";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final String username;
    private final String email;
    private final String password;
    private final String displayName;

    public AdminSeeder(AuthService authService, UserRepository userRepository,
                       @Value("${ADMIN_USERNAME:admin}") String username,
                       @Value("${ADMIN_EMAIL:admin@taekwondo.local}") String email,
                       @Value("${ADMIN_PASSWORD:" + DEFAULT_PASSWORD + "}") String password,
                       @Value("${ADMIN_DISPLAY_NAME:Administrador}") String displayName) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.username = username;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }
        authService.createAdmin(username, email, password, displayName);
        log.info("Admin inicial creado: {} / {}", username, email);
        if (DEFAULT_PASSWORD.equals(password)) {
            log.warn("El admin inicial usa la contraseña por defecto. Define ADMIN_PASSWORD fuera de desarrollo.");
        }
    }
}
