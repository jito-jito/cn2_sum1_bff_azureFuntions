package com.empresa.functions.roles.dto;

/**
 * Vista minima de un usuario desde la perspectiva del dominio roles (relacion
 * inversa Rol -> usuarios asignados). Deliberadamente no reutiliza
 * usuarios.dto.UsuarioDto para no acoplar el paquete roles al paquete
 * usuarios; cada dominio expone solo lo que necesita de si mismo.
 */
public record UsuarioResumenDto(Long id, String username, String email, String estado) {
}
