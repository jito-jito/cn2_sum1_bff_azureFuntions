package com.empresa.functions.common.eventos;

import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.empresa.functions.common.JsonUtil;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Evento de dominio a publicar, independiente del SDK de Event Grid. El
 * "data" del EventGridEvent resultante siempre tiene la misma forma
 * ({entidad, entidadId, correlationId, payload}) para que las consumidoras
 * puedan procesar cualquier tipo de evento sin conocer cada DTO.
 */
public record EventoDominio(
        String eventType,
        String subject,
        String entidad,
        Long entidadId,
        String correlationId,
        Object payload) {

    public static final String DATA_VERSION = "1.0";

    public EventGridEvent toEventGridEvent() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entidad", entidad);
        data.put("entidadId", entidadId);
        data.put("correlationId", correlationId);
        // Se convierte con nuestro ObjectMapper (Instant -> ISO-8601) antes de
        // entregarlo al SDK, para no depender de cómo serializa azure-core
        // tipos de java.time dentro de los DTOs.
        data.put("payload", payload == null ? null : JsonUtil.mapper().convertValue(payload, Object.class));
        return new EventGridEvent(subject, eventType, BinaryData.fromObject(data), DATA_VERSION);
    }
}
