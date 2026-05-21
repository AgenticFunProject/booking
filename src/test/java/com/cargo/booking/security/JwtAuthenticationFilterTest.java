package com.cargo.booking.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private final JwtTokenProvider jwtTokenProvider = org.mockito.Mockito.mock(JwtTokenProvider.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueUnauthenticatedWhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void shouldContinueUnauthenticatedWhenAuthorizationHeaderIsNotBearer() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic credentials");
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isSameAs(request);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void shouldPopulateSecurityContextForValidBearerToken() throws ServletException, IOException {
        String token = "valid.jwt.token";
        MockHttpServletRequest request = requestWithBearerToken(token);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user-123",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(token)).thenReturn(authentication);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getAuthentication(token);
    }

    @Test
    void shouldContinueUnauthenticatedForInvalidBearerToken() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        MockHttpServletRequest request = requestWithBearerToken(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider, never()).getAuthentication(token);
    }

    @Test
    void shouldContinueUnauthenticatedWhenAuthenticationExtractionFails() throws ServletException, IOException {
        String token = "malformed.jwt.token";
        MockHttpServletRequest request = requestWithBearerToken(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(token)).thenThrow(new MalformedJwtException("malformed"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getAuthentication(token);
    }

    @Test
    void shouldNotReplaceExistingAuthentication() throws ServletException, IOException {
        String token = "valid.jwt.token";
        MockHttpServletRequest request = requestWithBearerToken(token);
        Authentication existingAuthentication = new UsernamePasswordAuthenticationToken(
                "existing-user",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
        );
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existingAuthentication);
        verifyNoInteractions(jwtTokenProvider);
    }

    private MockHttpServletRequest requestWithBearerToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }
}
