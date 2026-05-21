package com.cargo.booking.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.cargo.booking.exception.ErrorResponseBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class JwtAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);

    @Test
    void shouldReturnStructuredUnauthorizedResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings");
        request.addHeader(ErrorResponseBuilder.REQUEST_ID_HEADER, "req-401");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("raw JWT parse detail"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("timestamp").asText()).isNotBlank();
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asText()).isEqualTo("Unauthorized");
        assertThat(body.get("message").asText()).isEqualTo("Authentication is required to access this resource");
        assertThat(body.get("message").asText()).doesNotContain("raw JWT parse detail");
        assertThat(body.get("path").asText()).isEqualTo("/api/v1/bookings");
        assertThat(body.get("requestId").asText()).isEqualTo("req-401");
    }
}
