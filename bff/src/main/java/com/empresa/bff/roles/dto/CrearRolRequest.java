package com.empresa.bff.roles.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearRolRequest(@NotBlank String nombre, String descripcion) {
}
