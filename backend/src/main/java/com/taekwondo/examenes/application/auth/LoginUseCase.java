package com.taekwondo.examenes.application.auth;

import com.taekwondo.examenes.application.auth.dto.LoginInput;
import com.taekwondo.examenes.application.auth.dto.LoginOutput;
import com.taekwondo.examenes.application.auth.dto.UserView;
import com.taekwondo.examenes.application.shared.exception.InvalidCredentialsException;
import com.taekwondo.examenes.domain.model.User;
import com.taekwondo.examenes.domain.port.JwtTokenProvider;
import com.taekwondo.examenes.domain.port.PasswordHasher;
import com.taekwondo.examenes.domain.port.UserRepository;

import java.util.Optional;

/**
 * Caso de uso: iniciar sesión.
 *
 * Cualquier fallo (usuario no existe, desactivado, contraseña incorrecta)
 * lanza la MISMA excepción con el MISMO mensaje, para evitar enumeración
 * de usuarios válidos del sistema.
 */
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginUseCase(UserRepository userRepository,
                         PasswordHasher passwordHasher,
                         JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginOutput execute(LoginInput input) {
        User user = findUserByUsernameOrEmail(input.usernameOrEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordHasher.matches(input.plainPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenProvider.generateToken(user.getId());
        return new LoginOutput(token, UserView.from(user));
    }

    private Optional<User> findUserByUsernameOrEmail(String usernameOrEmail) {
        Optional<User> byUsername = userRepository.findByUsername(usernameOrEmail);
        if (byUsername.isPresent()) return byUsername;
        return userRepository.findByEmail(usernameOrEmail);
    }
}
