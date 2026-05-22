package com.cargo.booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cargo.booking.security.AuthenticatedRequester;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthenticatedRequesterMdcFilterTest {

    private final AuthenticatedRequesterMdcFilter filter = new AuthenticatedRequesterMdcFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void shouldPopulateAuthenticatedRequesterMdcForFilterChain() throws ServletException, IOException {
        AuthenticatedRequester requester = new AuthenticatedRequester(
                "customer-subject",
                3001L,
                "customer.one",
                List.of("ROLE_CUSTOMER")
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                requester,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        ));

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
            assertThat(MDC.get(RequestTracingMdc.PRINCIPAL)).isEqualTo("customer-subject");
            assertThat(MDC.get(RequestTracingMdc.CUSTOMER_ID)).isEqualTo("3001");
        });

        assertThat(MDC.get(RequestTracingMdc.PRINCIPAL)).isNull();
        assertThat(MDC.get(RequestTracingMdc.CUSTOMER_ID)).isNull();
    }

    @Test
    void shouldClearStaleAuthenticatedRequesterMdcWhenUnauthenticated() throws ServletException, IOException {
        MDC.put(RequestTracingMdc.PRINCIPAL, "stale-user");
        MDC.put(RequestTracingMdc.CUSTOMER_ID, "9999");

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
            assertThat(MDC.get(RequestTracingMdc.PRINCIPAL)).isNull();
            assertThat(MDC.get(RequestTracingMdc.CUSTOMER_ID)).isNull();
        });

        assertThat(MDC.get(RequestTracingMdc.PRINCIPAL)).isNull();
        assertThat(MDC.get(RequestTracingMdc.CUSTOMER_ID)).isNull();
    }
}
