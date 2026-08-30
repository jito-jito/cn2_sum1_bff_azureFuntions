package com.empresa.functions.usuarios.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarRolRequest(@NotNull Long rolId) {
}
