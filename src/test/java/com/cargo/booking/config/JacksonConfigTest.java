package com.cargo.booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class JacksonConfigTest {

    private final JacksonConfig jacksonConfig = new JacksonConfig();

    @Test
    void shouldConfigureGlobalApiSerializationRules() throws Exception {
        ObjectMapper objectMapper = configuredObjectMapper();
        SampleResponse response = new SampleResponse(
                Instant.parse("2026-04-01T10:00:00Z"),
                "BKG-2026-00042",
                null);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.path("timestamp").asText()).isEqualTo("2026-04-01T10:00:00Z");
        assertThat(json.path("bookingReference").asText()).isEqualTo("BKG-2026-00042");
        assertThat(json.has("optionalNote")).isFalse();
    }

    @Test
    void shouldIgnoreUnknownRequestFields() throws Exception {
        ObjectMapper objectMapper = configuredObjectMapper();

        SampleRequest request = objectMapper.readValue(
                """
                {
                  "bookingReference": "BKG-2026-00042",
                  "unexpected": "ignored"
                }
                """,
                SampleRequest.class);

        assertThat(request.bookingReference()).isEqualTo("BKG-2026-00042");
    }

    private ObjectMapper configuredObjectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        jacksonConfig.bookingJacksonCustomizer().customize(builder);
        return builder.build();
    }

    private record SampleResponse(Instant timestamp, String bookingReference, String optionalNote) {
    }

    private record SampleRequest(String bookingReference) {
    }
}
