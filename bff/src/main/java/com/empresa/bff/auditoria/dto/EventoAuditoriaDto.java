package com.empresa.bff.auditoria.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * Evento de dominio registrado por la consumidora AuditarEvento. "data" se
 * expone como JSON libre porque su "payload" cambia según el tipo de evento
 * (UsuarioDto, RolDto o {usuarioId, rolId}).
 */
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
