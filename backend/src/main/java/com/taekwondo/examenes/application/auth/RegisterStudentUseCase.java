package com.taekwondo.examenes.application.auth;

import com.taekwondo.examenes.application.auth.dto.RegisterStudentInput;
import com.taekwondo.examenes.application.auth.dto.UserView;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.domain.model.User;
import com.taekwondo.examenes.domain.model.UserRole;
import com.taekwondo.examenes.domain.port.PasswordHasher;
import com.taekwondo.examenes.domain.port.UserRepository;

/**
 * Caso de uso: registro libre de un estudiante.
 *
 * Cualquier visitante puede registrarse. El rol resultante es siempre
 * STUDENT: no se acepta el rol como parámetro de entrada para evitar
 * que el frontend pueda solicitar la creación de un ADMIN.
 *
 * Diferencias con CreateUserUseCase:
 *  - CreateUserUseCase crea ADMINs (uso interno/seed).
 *  - RegisterStudentUseCase crea STUDENTs (endpoint público).
 *
 * Pasos:
 *  1. Comprobar unicidad de username y email.
 *  2. Hashear la contraseña.
 *  3. Construir la entidad User con rol STUDENT (fijado en el caso de uso).
 *  4. Persistir y devolver vista.
 */
public class RegisterStudentUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterStudentUseCase(UserRepository userRepository,
                                    PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public UserView execute(RegisterStudentInput input) {
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
                UserRole.STUDENT
        );

        User persisted = userRepository.save(user);
        return UserView.from(persisted);
    }
}
