package com.empresa.bff.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ActualizarUsuarioRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String nombreCompleto) {
}
