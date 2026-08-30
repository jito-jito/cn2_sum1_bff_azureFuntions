package com.empresa.bff.roles.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarRolRequest(@NotBlank String nombre, String descripcion) {
}
