package com.empresa.functions.common.eventos;

import com.empresa.functions.common.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * Evento tal como lo entrega un @EventGridTrigger (Event Grid Schema: id,
 * topic, subject, eventType, eventTime, data, dataVersion). Lo usan las
 * funciones consumidoras para no repetir el parseo del JSON en cada una.
 */
public record EventoRecibido(
        String id,
        String topic,
        String subject,
        String eventType,
        String eventTime,
        String dataVersion,
        JsonNode data) {

    public static EventoRecibido parse(String content) {
        try {
            return JsonUtil.mapper().readValue(content, EventoRecibido.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Evento de Event Grid con JSON inválido", e);
        }
    }

    public Instant eventTimeInstant() {
        return eventTime == null ? Instant.now() : Instant.parse(eventTime);
    }

    public String entidad() {
        return dataText("entidad");
    }

    public Long entidadId() {
        JsonNode node = data == null ? null : data.get("entidadId");
        return node == null || node.isNull() ? null : node.asLong();
    }

    public String correlationId() {
        return dataText("correlationId");
    }

    private String dataText(String field) {
        JsonNode node = data == null ? null : data.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }
}
