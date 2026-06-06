package com.taekwondo.examenes.infrastructure.web.controller;

import com.taekwondo.examenes.application.auth.ListUsersUseCase;
import com.taekwondo.examenes.application.auth.PromoteUserUseCase;
import com.taekwondo.examenes.application.auth.dto.UserView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de gestión de usuarios.
 * Solo accesibles por ADMIN (configurado en SecurityConfig).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ListUsersUseCase listUsersUseCase;
    private final PromoteUserUseCase promoteUserUseCase;

    public UserController(ListUsersUseCase listUsersUseCase,
                           PromoteUserUseCase promoteUserUseCase) {
        this.listUsersUseCase = listUsersUseCase;
        this.promoteUserUseCase = promoteUserUseCase;
    }

    @GetMapping
    public List<UserView> listAll() {
        return listUsersUseCase.execute();
    }

    @PatchMapping("/{id}/promote")
    public ResponseEntity<UserView> promote(@PathVariable Long id) {
        return ResponseEntity.ok(promoteUserUseCase.promote(id));
    }

    @PatchMapping("/{id}/demote")
    public ResponseEntity<UserView> demote(@PathVariable Long id) {
        return ResponseEntity.ok(promoteUserUseCase.demote(id));
    }
}
