package com.taekwondo.examenes.infrastructure.web.controller;

import com.taekwondo.examenes.application.auth.GetCurrentUserUseCase;
import com.taekwondo.examenes.application.auth.LoginUseCase;
import com.taekwondo.examenes.application.auth.RegisterStudentUseCase;
import com.taekwondo.examenes.application.auth.dto.LoginInput;
import com.taekwondo.examenes.application.auth.dto.LoginOutput;
import com.taekwondo.examenes.application.auth.dto.RegisterStudentInput;
import com.taekwondo.examenes.application.auth.dto.UserView;
import com.taekwondo.examenes.application.shared.exception.InvalidCredentialsException;
import com.taekwondo.examenes.infrastructure.web.dto.LoginRequest;
import com.taekwondo.examenes.infrastructure.web.dto.LoginResponse;
import com.taekwondo.examenes.infrastructure.web.dto.RegisterStudentRequest;
import com.taekwondo.examenes.infrastructure.web.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para autenticación.
 *
 * Endpoints:
 *  - POST /api/auth/register : registro público de estudiante
 *  - POST /api/auth/login    : login, devuelve JWT
 *  - GET  /api/auth/me       : datos del usuario autenticado
 *
 * /register y /login son públicos por contrato.
 *
 * /me lee el token del header "Authorization: Bearer <token>" y delega
 * en GetCurrentUserUseCase, que valida el token y devuelve la información
 * del usuario. NO depende del filtro JWT del SecurityContext, por lo que
 * funciona desde ya (antes del sub-paso E).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RegisterStudentUseCase registerStudentUseCase;
    private final LoginUseCase loginUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public AuthController(RegisterStudentUseCase registerStudentUseCase,
                           LoginUseCase loginUseCase,
                           GetCurrentUserUseCase getCurrentUserUseCase) {
        this.registerStudentUseCase = registerStudentUseCase;
        this.loginUseCase = loginUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterStudentRequest request) {
        RegisterStudentInput input = new RegisterStudentInput(
                request.username(),
                request.email(),
                request.plainPassword(),
                request.displayName()
        );
        UserView view = registerStudentUseCase.execute(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(view));
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        LoginInput input = new LoginInput(
                request.usernameOrEmail(),
                request.plainPassword()
        );
        LoginOutput output = loginUseCase.execute(input);
        return LoginResponse.from(output);
    }

    @GetMapping("/me")
    public UserResponse me(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractBearerToken(authHeader);
        UserView view = getCurrentUserUseCase.execute(token);
        return UserResponse.from(view);
    }

    /**
     * Extrae el token del header "Authorization: Bearer <token>".
     * Si no hay header o no empieza por "Bearer ", lanza
     * InvalidCredentialsException (el handler la traduce a 401).
     */
    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new InvalidCredentialsException();
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new InvalidCredentialsException();
        }
        return token;
    }
}
