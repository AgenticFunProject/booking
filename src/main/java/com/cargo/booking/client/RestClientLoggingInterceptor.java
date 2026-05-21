package com.cargo.booking.client;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RestClientLoggingInterceptor.class);
    private static final String REDACTED = "[REDACTED]";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Outbound request: {} {}", request.getMethod(), request.getURI());
            log.debug("Outbound request headers: {}", redactedHeaders(request.getHeaders()));
        }

        long startNanos = System.nanoTime();
        ClientHttpResponse response = execution.execute(request, body);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        if (log.isDebugEnabled()) {
            log.debug("Outbound response: {} in {}ms", response.getStatusCode().value(), elapsedMs);
        }

        return response;
    }

    private HttpHeaders redactedHeaders(HttpHeaders headers) {
        HttpHeaders redactedHeaders = new HttpHeaders();
        headers.forEach((name, values) -> redactedHeaders.put(name, redactedValues(name, values)));
        return redactedHeaders;
    }

    private List<String> redactedValues(String name, List<String> values) {
        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
            return List.of(REDACTED);
        }
        return List.copyOf(values);
    }
}
