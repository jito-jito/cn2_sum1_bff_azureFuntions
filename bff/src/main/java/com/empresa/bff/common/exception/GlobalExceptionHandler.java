package com.empresa.bff.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Traduce fallos de validación y de las llamadas a Azure Functions a un
 * contrato de error único. Los errores 4xx/5xx que ya vienen de una
 * Function se propagan con el mismo status: no se inventa una traducción
 * de errores distinta (ver docs/gestion-usuarios-roles.md §7).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Error de validación", request.getRequestURI(), details);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiError> handleUpstreamError(RestClientResponseException ex, HttpServletRequest request) {
        HttpStatusCode status = ex.getStatusCode();
        log.warn("La Azure Function respondió {} para {}", status, request.getRequestURI());
        ApiError body = ApiError.of(status.value(), status.toString(), extractUpstreamMessage(ex), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Las Azure Functions responden {"status","message","timestamp"} en sus
     * errores (ver HttpResponseUtil.error en azure-functions). Se extrae el
     * "message" para no exponer el JSON crudo anidado dentro del ApiError.
     */
    private String extractUpstreamMessage(RestClientResponseException ex) {
        try {
            JsonNode node = objectMapper.readTree(ex.getResponseBodyAsString());
            if (node.hasNonNull("message")) {
                return node.get("message").asText();
            }
        } catch (Exception ignored) {
            // body no era el JSON esperado: se cae al mensaje crudo de abajo
        }
        return ex.getResponseBodyAsString();
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiError> handleCircuitOpen(CallNotPermittedException ex, HttpServletRequest request) {
        log.error("Circuit breaker abierto: {}", ex.getMessage());
        ApiError body = ApiError.of(HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable",
                "Servicio no disponible temporalmente, intenta más tarde", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiError> handleTimeout(ResourceAccessException ex, HttpServletRequest request) {
        log.error("Timeout o error de conexión hacia Azure Functions: {}", ex.getMessage());
        ApiError body = ApiError.of(HttpStatus.GATEWAY_TIMEOUT.value(), "Gateway Timeout",
                "Tiempo de espera agotado al llamar a una Azure Function", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        ApiError body = ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                "Error interno inesperado", request.getRequestURI());
        return ResponseEntity.internalServerError().body(body);
    }
}
