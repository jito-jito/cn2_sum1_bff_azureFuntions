package com.empresa.bff.usuarios.dto;

import com.empresa.bff.roles.dto.RolDto;
import java.time.Instant;
import java.util.List;

public record UsuarioDto(
        Long id,
        String username,
        String email,
        String nombreCompleto,
        String estado,
        List<RolDto> roles,
        Instant fechaCreacion,
        Instant fechaModificacion) {
}
