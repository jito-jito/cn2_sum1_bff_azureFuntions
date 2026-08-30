package com.empresa.bff.usuarios.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarRolRequest(@NotNull Long rolId) {
}
