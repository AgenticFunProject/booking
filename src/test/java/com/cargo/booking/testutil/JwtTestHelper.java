package com.cargo.booking.testutil;

import com.cargo.booking.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.util.StringUtils;

public final class JwtTestHelper {

    public static final String DEFAULT_ISSUER = "platform-auth";
    public static final String DEFAULT_AUDIENCE = "equipments-service";
    public static final String DEFAULT_SECRET = "test-secret-key-that-is-at-least-256-bits-long";
    public static final String WRONG_SECRET = "different-secret-key-that-is-at-least-256-bits";
    public static final Duration DEFAULT_EXPIRATION = Duration.ofHours(1);
    public static final Long DEFAULT_CUSTOMER_ID = 3001L;
    public static final String DEFAULT_ADMIN_SUBJECT = "users-42";

    private JwtTestHelper() {
    }

    public static JwtProperties jwtProperties() {
        return jwtProperties(secret());
    }

    public static JwtProperties jwtProperties(String secret) {
        return new JwtProperties(
                DEFAULT_ISSUER,
                DEFAULT_AUDIENCE,
                secret,
                DEFAULT_EXPIRATION.toMillis()
        );
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    public static String generateToken(String subject, String username, List<String> roles) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("username", username);
        claims.put("roles", roles);
        return token(subject, claims);
    }

    public static String generateCustomerToken(Long customerId) {
        return generateCustomerToken("customer-" + customerId, customerId);
    }

    public static String generateCustomerToken(String subject, Long customerId) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("username", subject);
        claims.put("roles", List.of("CUSTOMER"));
        claims.put("customerId", customerId);
        return token(subject, claims);
    }

    public static String generateCustomerTokenWithSnakeCaseClaim(Long customerId) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("username", "customer-" + customerId);
        claims.put("roles", List.of("CUSTOMER"));
        claims.put("customer_id", customerId);
        return token("customer-" + customerId, claims);
    }

    public static String generateCustomerTokenMissingCustomerClaim(String subject) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("username", subject);
        claims.put("roles", List.of("CUSTOMER"));
        return token(subject, claims);
    }

    public static String generateServiceToken(String serviceName) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("username", serviceName);
        claims.put("roles", List.of("SERVICE"));
        return token(serviceName, claims);
    }

    public static String generateOperatorToken(String subject) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("username", subject);
        claims.put("roles", List.of("OPERATOR"));
        return token(subject, claims);
    }

    public static String generateAdminToken(String subject) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("name", "Users Admin");
        claims.put("role", "admin");
        return token(subject, claims);
    }

    public static String generateAdminToken() {
        return generateAdminToken(DEFAULT_ADMIN_SUBJECT);
    }

    public static String generateExpiredToken(String subject) {
        return token(subject, Map.of("roles", List.of("CUSTOMER")), Instant.now().minus(Duration.ofMinutes(1)));
    }

    public static String generateWrongIssuerToken(String subject) {
        return token(subject, Map.of("roles", List.of("CUSTOMER")), "wrong-issuer", DEFAULT_AUDIENCE,
                Instant.now().plus(DEFAULT_EXPIRATION), secret());
    }

    public static String generateWrongAudienceToken(String subject) {
        return token(subject, Map.of("roles", List.of("CUSTOMER")), DEFAULT_ISSUER, "users-service",
                Instant.now().plus(DEFAULT_EXPIRATION), secret());
    }

    public static String generateInvalidSignatureToken(String subject) {
        return token(subject, Map.of("roles", List.of("CUSTOMER")), DEFAULT_ISSUER, DEFAULT_AUDIENCE,
                Instant.now().plus(DEFAULT_EXPIRATION), WRONG_SECRET);
    }

    public static String generateMissingSubjectToken() {
        return token(null, Map.of("roles", List.of("CUSTOMER")), Instant.now().plus(DEFAULT_EXPIRATION));
    }

    public static String generateMalformedToken() {
        return "not-a-jwt";
    }

    public static String token(String subject, Map<String, Object> claims) {
        return token(subject, claims, Instant.now().plus(DEFAULT_EXPIRATION));
    }

    public static String token(String subject, Map<String, Object> claims, Instant expiration) {
        return token(subject, claims, DEFAULT_ISSUER, DEFAULT_AUDIENCE, expiration, secret());
    }

    public static String token(
            String subject,
            Map<String, Object> claims,
            String issuer,
            String audience,
            Instant expiration,
            String secret
    ) {
        var builder = Jwts.builder()
                .claims(claims)
                .issuer(issuer)
                .audience().add(audience).and()
                .expiration(Date.from(expiration))
                .signWith(signingKey(secret));

        if (StringUtils.hasText(subject)) {
            builder.subject(subject);
        }

        return builder.compact();
    }

    private static String secret() {
        String configuredSecret = System.getenv("AUTH_JWT_SECRET");
        return StringUtils.hasText(configuredSecret) ? configuredSecret : DEFAULT_SECRET;
    }

    private static SecretKey signingKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
