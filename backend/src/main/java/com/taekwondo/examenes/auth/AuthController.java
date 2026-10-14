package com.taekwondo.examenes.auth;

import com.taekwondo.examenes.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
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
