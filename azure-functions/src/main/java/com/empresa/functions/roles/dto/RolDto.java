package com.empresa.functions.roles.dto;

import java.time.Instant;

public record RolDto(
        Long id,
        String nombre,
        String descripcion,
        String estado,
        Instant fechaCreacion) {
}
