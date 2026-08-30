package com.empresa.bff.config;

import com.empresa.bff.common.web.CorrelationIdRequestInterceptor;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AzureFunctionsClientConfig {

    @Bean
    public RestClient azureFunctionsRestClient(
            AzureFunctionsProperties properties,
            CorrelationIdRequestInterceptor correlationIdRequestInterceptor) {

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(correlationIdRequestInterceptor)
                // Requerido por las Functions desplegadas en Azure (authLevel
                // FUNCTION). En local con `func start` esta cabecera se ignora.
                .defaultHeader("x-functions-key", properties.functionKey())
                .build();
    }
}
