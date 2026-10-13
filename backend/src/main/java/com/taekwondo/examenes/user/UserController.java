package com.taekwondo.examenes.user;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestión de usuarios. Solo ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request,
                                   @AuthenticationPrincipal Long userId) {
        return userService.changeRole(id, request.role(), userId);
    }
}
