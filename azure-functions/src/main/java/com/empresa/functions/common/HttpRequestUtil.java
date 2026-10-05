package com.empresa.functions.common;

import com.microsoft.azure.functions.HttpRequestMessage;
import java.util.Map;
import java.util.UUID;

public final class HttpRequestUtil {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private HttpRequestUtil() {
    }

    /**
     * Correlation-id que propaga el BFF (CorrelationIdRequestInterceptor), para
     * incluirlo en los eventos publicados y poder trazar un request hasta las
     * consumidoras. Búsqueda case-insensitive porque el worker de Java no
     * garantiza el casing original de los headers. Si la Function se invoca
     * directo (sin BFF), se genera uno nuevo.
     */
    public static String correlationId(HttpRequestMessage<?> request) {
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            if (CORRELATION_ID_HEADER.equalsIgnoreCase(header.getKey())
                    && header.getValue() != null && !header.getValue().isBlank()) {
                return header.getValue();
            }
        }
        return UUID.randomUUID().toString();
    }
}
