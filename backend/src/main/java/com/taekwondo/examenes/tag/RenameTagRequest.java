package com.taekwondo.examenes.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameTagRequest(@NotBlank @Size(max = 50) String newName) {}
