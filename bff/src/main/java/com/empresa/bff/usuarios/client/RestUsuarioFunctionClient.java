package com.empresa.bff.usuarios.client;

import com.empresa.bff.usuarios.dto.ActualizarUsuarioRequest;
import com.empresa.bff.usuarios.dto.CrearUsuarioRequest;
import com.empresa.bff.usuarios.dto.UsuarioDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestUsuarioFunctionClient implements UsuarioFunctionClient {

    private static final String INSTANCE = "usuariosFunction";

    private final RestClient restClient;

    public RestUsuarioFunctionClient(RestClient azureFunctionsRestClient) {
        this.restClient = azureFunctionsRestClient;
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    public UsuarioDto crear(CrearUsuarioRequest request) {
        return restClient.post()
                .uri("/api/usuarios")
                .body(request)
                .retrieve()
                .body(UsuarioDto.class);
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public List<UsuarioDto> listar() {
        return restClient.get()
                .uri("/api/usuarios")
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<UsuarioDto>>() {
                });
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public UsuarioDto obtener(Long id) {
        return restClient.get()
                .uri("/api/usuarios/{id}", id)
                .retrieve()
                .body(UsuarioDto.class);
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public UsuarioDto actualizar(Long id, ActualizarUsuarioRequest request) {
        return restClient.put()
                .uri("/api/usuarios/{id}", id)
                .body(request)
                .retrieve()
                .body(UsuarioDto.class);
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public void eliminar(Long id) {
        restClient.delete()
                .uri("/api/usuarios/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    public void asignarRol(Long usuarioId, Long rolId) {
        restClient.post()
                .uri("/api/usuarios/{id}/roles", usuarioId)
                .body(new AsignarRolBody(rolId))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    @CircuitBreaker(name = INSTANCE)
    @Retry(name = INSTANCE)
    public void quitarRol(Long usuarioId, Long rolId) {
        restClient.delete()
                .uri("/api/usuarios/{id}/roles/{rolId}", usuarioId, rolId)
                .retrieve()
                .toBodilessEntity();
    }

    private record AsignarRolBody(Long rolId) {
    }
}
