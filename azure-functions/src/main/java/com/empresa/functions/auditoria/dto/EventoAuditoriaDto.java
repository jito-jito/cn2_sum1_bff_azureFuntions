package com.empresa.functions.auditoria.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record EventoAuditoriaDto(
        String eventId,
        String eventType,
        String subject,
        String entidad,
        Long entidadId,
        String correlationId,
        JsonNode data,
        Instant eventTime,
        Instant fechaRegistro) {
}
