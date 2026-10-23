package com.taekwondo.examenes.auth;

import com.taekwondo.examenes.user.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** La misma respuesta exista o no la cuenta: no revela quién está registrado (RF-34). */
    static final String RECOVERY_SENT = "Si ese email está registrado, te hemos enviado un correo con tu usuario "
            + "y un enlace para cambiar la contraseña.";

    private final AuthService authService;
    private final AccountRecoveryService recoveryService;

    public AuthController(AuthService authService, AccountRecoveryService recoveryService) {
        this.authService = authService;
        this.recoveryService = recoveryService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }

    /** Sin sesión: envía el usuario y un enlace para la contraseña a quien tenga ese email (RF-34). */
    @PostMapping("/recovery")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> requestRecovery(@Valid @RequestBody RecoveryRequest request) {
        recoveryService.requestRecovery(request.email());
        return Map.of("message", RECOVERY_SENT);
    }

    /** Sin sesión: la contraseña nueva con el código del enlace (RF-34). */
    @PostMapping("/recovery/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetWithTokenRequest request) {
        recoveryService.resetPassword(request.token(), request.newPassword());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Long userId) {
        return authService.me(userId);
    }

    /** Devuelve una sesión nueva: las anteriores, también la de esta petición, dejan de valer. */
    @PutMapping("/me/password")
    public LoginResponse changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                        @AuthenticationPrincipal Long userId) {
        return authService.changeOwnPassword(userId, request);
    }
}
