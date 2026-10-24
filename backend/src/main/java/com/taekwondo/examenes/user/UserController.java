package com.taekwondo.examenes.user;

import com.taekwondo.examenes.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Gestión de usuarios. Solo ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Por páginas (RNF-09); q busca en el nombre, el usuario o el email (RF-37). */
    @GetMapping
    public PageResponse<UserResponse> search(@RequestParam(defaultValue = "") String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return userService.search(q, page, size);
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request,
                                   @AuthenticationPrincipal Long userId) {
        return userService.changeRole(id, request.role(), userId);
    }

    @PatchMapping("/{id}/active")
    public UserResponse setActive(@PathVariable Long id, @Valid @RequestBody SetActiveRequest request,
                                  @AuthenticationPrincipal Long userId) {
        return userService.setActive(id, request.active(), userId);
    }

    @PutMapping("/{id}/password")
    public UserResponse resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request,
                                      @AuthenticationPrincipal Long userId) {
        return userService.resetPassword(id, request.temporaryPassword(), userId);
    }
}
