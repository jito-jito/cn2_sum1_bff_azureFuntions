package com.empresa.bff.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "azure-functions")
public record AzureFunctionsProperties(
        String baseUrl,
        String functionKey,
        Duration connectTimeout,
        Duration readTimeout) {
}
