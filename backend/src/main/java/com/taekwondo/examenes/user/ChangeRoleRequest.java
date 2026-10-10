package com.taekwondo.examenes.user;

import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull UserRole role) {}
