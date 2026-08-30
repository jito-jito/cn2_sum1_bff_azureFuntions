package com.empresa.functions.roles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearRolRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 300) String descripcion) {
}
