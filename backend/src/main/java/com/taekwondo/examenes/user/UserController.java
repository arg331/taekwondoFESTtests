package com.taekwondo.examenes.user;

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

    @PatchMapping("/{id}/promote")
    public UserResponse promote(@PathVariable Long id) {
        return userService.promote(id);
    }

    @PatchMapping("/{id}/demote")
    public UserResponse demote(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return userService.demote(id, userId);
    }
}
