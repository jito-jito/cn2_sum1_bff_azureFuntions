package com.empresa.functions.common;

import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.Map;

public final class HttpResponseUtil {

    private HttpResponseUtil() {
    }

    public static HttpResponseMessage json(HttpRequestMessage<?> request, HttpStatus status, Object body) {
        try {
            String payload = body == null ? null : JsonUtil.mapper().writeValueAsString(body);
            var builder = request.createResponseBuilder(status)
                    .header("Content-Type", "application/json");
            return payload == null ? builder.build() : builder.body(payload).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("No se pudo serializar la respuesta", e);
        }
    }

    public static HttpResponseMessage error(HttpRequestMessage<?> request, HttpStatus status, String message) {
        return json(request, status, Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "message", message));
    }
}
