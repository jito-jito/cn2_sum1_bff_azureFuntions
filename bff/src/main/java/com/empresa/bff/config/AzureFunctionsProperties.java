package com.empresa.bff.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "azure-functions")
public record AzureFunctionsProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout) {
}
