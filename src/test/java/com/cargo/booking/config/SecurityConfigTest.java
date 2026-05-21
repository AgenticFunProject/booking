package com.cargo.booking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cargo.booking.security.JwtAccessDeniedHandler;
import com.cargo.booking.security.JwtAuthenticationEntryPoint;
import com.cargo.booking.security.JwtAuthenticationFilter;
import com.cargo.booking.security.JwtTokenProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTestController.class)
@ImportAutoConfiguration(exclude = org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
@TestPropertySource(properties = {
        "app.security.enabled=true",
        "app.cors.allowed-origins=http://localhost:3000,http://localhost:5173"
})
class SecurityConfigEnabledTest {

    private static final String ADMIN_TOKEN = "admin-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldAllowDocumentationAndHealthEndpointsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api-docs/openapi.json"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequireAuthenticationForProtectedBookingEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    @Test
    void shouldEnforceBookingEndpointRoleRules() throws Exception {
        mockMvc.perform(post("/api/v1/bookings").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/bookings").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/bookings/42/confirm").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/bookings/42/confirm").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldProtectMetricsWithAdminRole() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/metrics").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/metrics").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAuthorizeAdminJwtForProtectedBookingRoutes() throws Exception {
        when(jwtTokenProvider.validateToken(ADMIN_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(ADMIN_TOKEN)).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        "admin-user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        mockMvc.perform(patch("/api/v1/bookings/42/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void shouldApplyCorsConfiguration() throws Exception {
        mockMvc.perform(options("/api/v1/bookings")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                        .isEqualTo("http://localhost:5173"))
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                        .isEqualTo("true"));
    }

}

@WebMvcTest(controllers = SecurityConfigTestController.class)
@ImportAutoConfiguration(exclude = org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
@TestPropertySource(properties = "app.security.enabled=false")
class SecurityConfigDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldPermitAllRequestsAndSkipJwtValidationWhenSecurityIsDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isOk());

        verifyNoInteractions(jwtTokenProvider);
    }

}

@RestController
class SecurityConfigTestController {

    @GetMapping({
            "/swagger-ui/index.html",
            "/api-docs/openapi.json",
            "/actuator/health",
            "/actuator/info",
            "/actuator/metrics",
            "/api/v1/bookings",
            "/api/v1/bookings/42"
    })
    String getEndpoint() {
        return "ok";
    }

    @PostMapping("/api/v1/bookings")
    String createBooking() {
        return "ok";
    }

    @PatchMapping({
            "/api/v1/bookings/42/cancel",
            "/api/v1/bookings/42/confirm",
            "/api/v1/bookings/42/start",
            "/api/v1/bookings/42/complete"
    })
    String lifecycleEndpoint() {
        return "ok";
    }
}
