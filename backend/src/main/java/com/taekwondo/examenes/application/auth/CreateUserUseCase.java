package com.taekwondo.examenes.application.auth;

import com.taekwondo.examenes.application.auth.dto.CreateUserInput;
import com.taekwondo.examenes.application.auth.dto.UserView;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.domain.model.User;
import com.taekwondo.examenes.domain.model.UserRole;
import com.taekwondo.examenes.domain.port.PasswordHasher;
import com.taekwondo.examenes.domain.port.UserRepository;

/**
 * Caso de uso: crear un nuevo usuario ADMINISTRADOR.
 *
 * Pensado para:
 *  - Seed inicial (crear el primer admin del sistema desde un script o test).
 *  - Si en el futuro un admin necesita crear otros admins.
 *
 * Para registro libre de estudiantes existe RegisterStudentUseCase
 * (caso de uso distinto: distintos permisos, distinto rol, distinta política).
 *
 * Pasos:
 *  1. Comprobar unicidad de username y email.
 *  2. Hashear la contraseña.
 *  3. Construir la entidad User con rol ADMIN.
 *  4. Persistir y devolver vista.
 */
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public CreateUserUseCase(UserRepository userRepository,
                              PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public UserView execute(CreateUserInput input) {
        if (userRepository.existsByUsername(input.username())) {
            throw new BusinessRuleViolationException(
                    "Ya existe un usuario con username '" + input.username() + "'");
        }
        if (userRepository.existsByEmail(input.email())) {
            throw new BusinessRuleViolationException(
                    "Ya existe un usuario con email '" + input.email() + "'");
        }

        String passwordHash = passwordHasher.hash(input.plainPassword());

        User user = User.createNew(
                input.username(),
                input.email(),
                passwordHash,
                input.displayName(),
                UserRole.ADMIN
        );

        User persisted = userRepository.save(user);
        return UserView.from(persisted);
    }
}
