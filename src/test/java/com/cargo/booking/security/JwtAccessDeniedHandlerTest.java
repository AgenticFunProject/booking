package com.cargo.booking.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class JwtAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final JwtAccessDeniedHandler accessDeniedHandler = new JwtAccessDeniedHandler(objectMapper);

    @Test
    void shouldReturnStructuredForbiddenResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/bookings/42/confirm");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("ROLE_CUSTOMER is not allowed"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("timestamp").asText()).isNotBlank();
        assertThat(body.get("status").asInt()).isEqualTo(403);
        assertThat(body.get("error").asText()).isEqualTo("Forbidden");
        assertThat(body.get("message").asText()).isEqualTo("You do not have permission to perform this action");
        assertThat(body.get("message").asText()).doesNotContain("ROLE_CUSTOMER");
        assertThat(body.get("path").asText()).isEqualTo("/api/v1/bookings/42/confirm");
        assertThat(body.has("requestId")).isFalse();
    }
}
