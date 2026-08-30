package com.empresa.bff.common.web;

import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/**
 * Propaga el correlation-id del request entrante hacia cada llamada saliente
 * a las Azure Functions, para poder trazar un request de punta a punta.
 */
@Component
public class CorrelationIdRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            request.getHeaders().add(CorrelationIdFilter.HEADER_NAME, correlationId);
        }
        return execution.execute(request, body);
    }
}
