package com.cargo.booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestTracingMdcFilterTest {

    private final RequestTracingMdcFilter filter = new RequestTracingMdcFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldUseRequestIdHeaderAndClearTracingMdcAfterRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestTracingMdcFilter.REQUEST_ID_HEADER, " req-123 ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get(RequestTracingMdc.REQUEST_ID)).isEqualTo("req-123");
            MDC.put(RequestTracingMdc.PRINCIPAL, "user-123");
            MDC.put(RequestTracingMdc.CUSTOMER_ID, "3001");
            MDC.put(RequestTracingMdc.BOOKING_REF, "BKG-2026-00042");
        });

        assertThat(response.getHeader(RequestTracingMdcFilter.REQUEST_ID_HEADER)).isEqualTo("req-123");
        assertThat(MDC.get(RequestTracingMdc.REQUEST_ID)).isNull();
        assertThat(MDC.get(RequestTracingMdc.PRINCIPAL)).isNull();
        assertThat(MDC.get(RequestTracingMdc.CUSTOMER_ID)).isNull();
        assertThat(MDC.get(RequestTracingMdc.BOOKING_REF)).isNull();
    }

    @Test
    void shouldGenerateRequestIdWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(RequestTracingMdc.REQUEST_ID)).isNotBlank()
        );

        assertThat(response.getHeader(RequestTracingMdcFilter.REQUEST_ID_HEADER)).isNotBlank();
        assertThat(MDC.get(RequestTracingMdc.REQUEST_ID)).isNull();
    }
}
