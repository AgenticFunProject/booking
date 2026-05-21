package com.cargo.booking.config;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(IntegrationProperties.class)
public class RestClientConfig {

    private final IntegrationProperties integrationProperties;
    private final List<ClientHttpRequestInterceptor> interceptors;

    public RestClientConfig(
            IntegrationProperties integrationProperties,
            ObjectProvider<ClientHttpRequestInterceptor> interceptors
    ) {
        this.integrationProperties = integrationProperties;
        this.interceptors = interceptors.orderedStream().toList();
    }

    @Bean
    @Qualifier("scheduleRestClient")
    public RestClient scheduleRestClient() {
        IntegrationProperties.ScheduleApi scheduleApi = integrationProperties.scheduleApi();
        return restClient(scheduleApi.baseUrl(), scheduleApi.timeoutMs());
    }

    @Bean
    @Qualifier("equipmentRestClient")
    public RestClient equipmentRestClient() {
        IntegrationProperties.EquipmentApi equipmentApi = integrationProperties.equipmentApi();
        return restClient(equipmentApi.baseUrl(), equipmentApi.timeoutMs());
    }

    @Bean
    @Qualifier("quoteRestClient")
    public RestClient quoteRestClient() {
        IntegrationProperties.QuoteApi quoteApi = integrationProperties.quoteApi();
        return restClient(quoteApi.baseUrl(), quoteApi.timeoutMs());
    }

    private RestClient restClient(String baseUrl, int timeoutMs) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(timeoutMs))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        interceptors.forEach(builder::requestInterceptor);
        return builder.build();
    }

    private ClientHttpRequestFactory requestFactory(int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        return requestFactory;
    }

    /*
     * When external API contracts are available, add DTOs in client.dto with
     * @JsonIgnoreProperties(ignoreUnknown = true), implement dev/prod client
     * services that inject these qualified RestClients, and protect calls with
     * named @CircuitBreaker/@Retry instances plus contract-backed WireMock tests.
     */
}
