package com.empresa.bff.config;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Un HealthIndicator por cada Azure Function crítica (ver CLAUDE.md,
 * "Arquitectura interna del BFF"), para poder distinguir en
 * /actuator/health cuál dependencia está fallando en vez de solo saber que
 * "algo" en el BFF no está saludable.
 */
@Configuration
public class HealthIndicatorsConfig {

    @Bean
    public HealthIndicator usuariosFunctionHealthIndicator(RestClient azureFunctionsRestClient) {
        return new AzureFunctionHealthIndicator(azureFunctionsRestClient, "/api/usuarios");
    }

    @Bean
    public HealthIndicator rolesFunctionHealthIndicator(RestClient azureFunctionsRestClient) {
        return new AzureFunctionHealthIndicator(azureFunctionsRestClient, "/api/roles");
    }
}
