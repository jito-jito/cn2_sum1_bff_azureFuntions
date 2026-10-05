package com.empresa.bff.auditoria.client;

import com.empresa.bff.auditoria.dto.EventoAuditoriaDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestAuditoriaFunctionClient implements AuditoriaFunctionClient {

    private static final String INSTANCE = "auditoriaFunction";

    private final RestClient restClient;

    public RestAuditoriaFunctionClient(RestClient azureFunctionsRestClient) {
        this.restClient = azureFunctionsRestClient;
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public List<EventoAuditoriaDto> listar(String entidad, Long entidadId, Integer limit) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/auditoria")
                        .queryParamIfPresent("entidad", Optional.ofNullable(entidad))
                        .queryParamIfPresent("entidadId", Optional.ofNullable(entidadId))
                        .queryParamIfPresent("limit", Optional.ofNullable(limit))
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<EventoAuditoriaDto>>() {
                });
    }
}
