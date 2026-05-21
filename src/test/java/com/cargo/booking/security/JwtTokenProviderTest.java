package com.cargo.booking.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long";

    private final JwtProperties jwtProperties = new JwtProperties(
            "platform-auth",
            "equipments-service",
            SECRET,
            Duration.ofHours(1)
    );

    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(jwtProperties);

    @Test
    void shouldValidateTokenAndExtractClaims() {
        String token = token(Map.of(
                "sub", "user-123",
                "username", "operator.one",
                "roles", List.of("CUSTOMER", "ROLE_SERVICE"),
                "customerId", 3001,
                "scope", "booking:read booking:write"
        ));

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getSubjectFromToken(token)).isEqualTo("user-123");
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("operator.one");
        assertThat(tokenProvider.getCustomerIdFromToken(token)).contains(3001L);
        assertThat(tokenProvider.getRolesFromToken(token)).containsExactly("ROLE_CUSTOMER", "ROLE_SERVICE");

        Authentication authentication = tokenProvider.getAuthentication(token);
        assertThat(authentication.getName()).isEqualTo("user-123");
        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthenticatedRequester(
                        "user-123",
                        3001L,
                        "operator.one",
                        List.of("ROLE_CUSTOMER", "ROLE_SERVICE")
                ));
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CUSTOMER", "ROLE_SERVICE", "SCOPE_booking:read", "SCOPE_booking:write");
    }

    @Test
    void shouldMapUsersAdminRoleClaimToRoleAdmin() {
        String token = token(Map.of(
                "sub", "admin-user",
                "name", "Admin User",
                "role", "admin",
                "customer_id", "4002"
        ));

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("Admin User");
        assertThat(tokenProvider.getCustomerIdFromToken(token)).contains(4002L);
        assertThat(tokenProvider.getRolesFromToken(token)).containsExactly("ROLE_ADMIN");
        assertThat(tokenProvider.getAuthentication(token).getPrincipal())
                .isEqualTo(new AuthenticatedRequester(
                        "admin-user",
                        4002L,
                        "Admin User",
                        List.of("ROLE_ADMIN")
                ));
        assertThat(tokenProvider.getAuthentication(token).getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldRejectTokenWithWrongIssuer() {
        String token = token("wrong-issuer", "equipments-service", Instant.now().plus(Duration.ofHours(1)));

        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void shouldRejectTokenWithWrongAudience() {
        String token = token("platform-auth", "other-service", Instant.now().plus(Duration.ofHours(1)));

        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        String token = token("platform-auth", "equipments-service", Instant.now().minus(Duration.ofMinutes(1)));

        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() {
        SecretKey wrongKey = Keys.hmacShaKeyFor("different-secret-key-that-is-at-least-256-bits".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user-123")
                .issuer("platform-auth")
                .audience().add("equipments-service").and()
                .expiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
                .signWith(wrongKey)
                .compact();

        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void shouldRejectMissingSubject() {
        String token = Jwts.builder()
                .issuer("platform-auth")
                .audience().add("equipments-service").and()
                .expiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
                .signWith(signingKey())
                .compact();

        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void shouldRejectBlankToken() {
        assertThat(tokenProvider.validateToken(" ")).isFalse();
    }

    private String token(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .issuer("platform-auth")
                .audience().add("equipments-service").and()
                .expiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
                .signWith(signingKey())
                .compact();
    }

    private String token(String issuer, String audience, Instant expiration) {
        return Jwts.builder()
                .subject("user-123")
                .issuer(issuer)
                .audience().add(audience).and()
                .expiration(Date.from(expiration))
                .signWith(signingKey())
                .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
