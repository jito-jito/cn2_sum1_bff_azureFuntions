package com.empresa.bff.roles.client;

import com.empresa.bff.roles.dto.ActualizarRolRequest;
import com.empresa.bff.roles.dto.CrearRolRequest;
import com.empresa.bff.roles.dto.RolDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestRolFunctionClient implements RolFunctionClient {

    private static final String INSTANCE = "rolesFunction";

    private final RestClient restClient;

    public RestRolFunctionClient(RestClient azureFunctionsRestClient) {
        this.restClient = azureFunctionsRestClient;
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    public RolDto crear(CrearRolRequest request) {
        return restClient.post()
                .uri("/api/roles")
                .body(request)
                .retrieve()
                .body(RolDto.class);
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public List<RolDto> listar() {
        return restClient.get()
                .uri("/api/roles")
                .retrieve()
                .body(new ParameterizedTypeReference<List<RolDto>>() {
                });
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public RolDto obtener(Long id) {
        return restClient.get()
                .uri("/api/roles/{id}", id)
                .retrieve()
                .body(RolDto.class);
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public RolDto actualizar(Long id, ActualizarRolRequest request) {
        return restClient.put()
                .uri("/api/roles/{id}", id)
                .body(request)
                .retrieve()
                .body(RolDto.class);
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public void eliminar(Long id) {
        restClient.delete()
                .uri("/api/roles/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
