package com.empresa.bff.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Health check de una Azure Function específica, para que Actuator reporte
 * cada dependencia por separado (no solo el health genérico de la app). Una
 * respuesta HTTP con error (4xx/5xx) o una excepción de conexión marcan la
 * dependencia como DOWN; cualquier respuesta 2xx la marca como UP.
 */
public class AzureFunctionHealthIndicator implements HealthIndicator {

    private final RestClient restClient;
    private final String probePath;

    public AzureFunctionHealthIndicator(RestClient restClient, String probePath) {
        this.restClient = restClient;
        this.probePath = probePath;
    }

    @Override
    public Health health() {
        try {
            restClient.get().uri(probePath).retrieve().toBodilessEntity();
            return Health.up().withDetail("probePath", probePath).build();
        } catch (RestClientResponseException e) {
            return Health.down()
                    .withDetail("probePath", probePath)
                    .withDetail("httpStatus", e.getStatusCode().value())
                    .build();
        } catch (Exception e) {
            return Health.down(e).withDetail("probePath", probePath).build();
        }
    }
}
