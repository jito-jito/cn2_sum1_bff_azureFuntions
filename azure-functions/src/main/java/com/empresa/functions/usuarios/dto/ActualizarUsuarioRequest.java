package com.empresa.functions.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 200) String nombreCompleto) {
}
