package com.cargo.booking.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

@TestConfiguration(proxyBeanMethods = false)
public class TestSecurityConfig {

    public static final String DISABLE_JWT_VALIDATION_PROPERTY = "app.security.enabled=false";

    private static final String ROLE_PREFIX = "ROLE_";

    public static Authentication authentication(
            String subject,
            Long customerId,
            String username,
            List<String> roles
    ) {
        List<String> normalizedRoles = roles.stream()
                .map(TestSecurityConfig::normalizeRole)
                .filter(StringUtils::hasText)
                .toList();
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        normalizedRoles.stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        AuthenticatedRequester principal = new AuthenticatedRequester(
                subject,
                customerId,
                username,
                normalizedRoles
        );
        return new UsernamePasswordAuthenticationToken(principal, null, List.copyOf(authorities));
    }

    public static void setAuthentication(
            String subject,
            Long customerId,
            String username,
            List<String> roles
    ) {
        SecurityContextHolder.getContext().setAuthentication(authentication(subject, customerId, username, roles));
    }

    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }

        String normalized = role.trim().toUpperCase();
        if (!normalized.startsWith(ROLE_PREFIX)) {
            normalized = ROLE_PREFIX + normalized;
        }
        return normalized;
    }
}
