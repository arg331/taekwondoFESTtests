package com.taekwondo.examenes.infrastructure.config;

import com.taekwondo.examenes.application.auth.CreateUserUseCase;
import com.taekwondo.examenes.application.auth.dto.CreateUserInput;
import com.taekwondo.examenes.domain.model.UserRole;
import com.taekwondo.examenes.domain.port.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seed inicial: crea el primer ADMIN si no existe ninguno.
 *
 * Idempotente: si ya existe cualquier usuario con rol ADMIN,
 * no hace nada (independientemente del username configurado).
 *
 * En producción, exportar antes de arrancar:
 *   export ADMIN_USERNAME="superadmin"
 *   export ADMIN_EMAIL="admin@mifederacion.com"
 *   export ADMIN_PASSWORD="$(openssl rand -base64 24)"
 *   export ADMIN_DISPLAY_NAME="Administrador"
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final CreateUserUseCase createUserUseCase;
    private final UserRepository userRepository;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_EMAIL:admin@taekwondo.local}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:admin123}")
    private String adminPassword;

    @Value("${ADMIN_DISPLAY_NAME:Administrador}")
    private String adminDisplayName;

    public AdminSeeder(CreateUserUseCase createUserUseCase,
                       UserRepository userRepository) {
        this.createUserUseCase = createUserUseCase;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            log.debug("Admin ya existe, seed omitido");
            return;
        }

        try {
            CreateUserInput input = new CreateUserInput(
                    adminUsername,
                    adminEmail,
                    adminPassword,
                    adminDisplayName
            );
            createUserUseCase.execute(input);
            log.info("✅ Admin inicial creado: {} / {}", adminUsername, adminEmail);
        } catch (Exception ex) {
            log.error("❌ Error al crear el admin inicial: {}", ex.getMessage());
        }
    }
}
