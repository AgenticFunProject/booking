package com.cargo.booking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String ROLE_PREFIX = "ROLE_";

    private static final String SCOPE_PREFIX = "SCOPE_";

    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!StringUtils.hasText(claims.getSubject())) {
                log.warn("JWT validation failed: subject claim is missing");
                return false;
            }
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT validation failed: token expired");
        } catch (MalformedJwtException ex) {
            log.warn("JWT validation failed: malformed token");
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT validation failed: unsupported token");
        } catch (SecurityException ex) {
            log.warn("JWT validation failed: invalid signature");
        } catch (IllegalArgumentException ex) {
            log.warn("JWT validation failed: token is blank or invalid");
        } catch (JwtException ex) {
            log.warn("JWT validation failed: {}", ex.getClass().getSimpleName());
        }
        return false;
    }

    public String getSubjectFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Optional<Long> getCustomerIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return readLongClaim(claims, "customerId")
                .or(() -> readLongClaim(claims, "customer_id"));
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseClaims(token);
        String username = readStringClaim(claims, "username");
        if (StringUtils.hasText(username)) {
            return username;
        }

        String name = readStringClaim(claims, "name");
        if (StringUtils.hasText(name)) {
            return name;
        }

        return claims.getSubject();
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = parseClaims(token);
        Set<String> roles = new LinkedHashSet<>();
        addRoles(roles, claims.get("roles"));
        addRoles(roles, claims.get("role"));
        return List.copyOf(roles);
    }

    public Authentication getAuthentication(String token) {
        String subject = getSubjectFromToken(token);
        Claims claims = parseClaims(token);
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        getRolesFromClaims(claims).stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        getScopesFromClaims(claims).stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return new UsernamePasswordAuthenticationToken(subject, null, List.copyOf(authorities));
    }

    private Claims parseClaims(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("JWT token must not be blank");
        }

        return Jwts.parser()
                .verifyWith(signingKey())
                .requireIssuer(jwtProperties.issuer())
                .requireAudience(jwtProperties.audience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        if (!StringUtils.hasText(jwtProperties.secret())) {
            throw new IllegalArgumentException("JWT secret must not be blank");
        }
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private List<String> getRolesFromClaims(Claims claims) {
        Set<String> roles = new LinkedHashSet<>();
        addRoles(roles, claims.get("roles"));
        addRoles(roles, claims.get("role"));
        return List.copyOf(roles);
    }

    private List<String> getScopesFromClaims(Claims claims) {
        Set<String> scopes = new LinkedHashSet<>();
        addScopes(scopes, claims.get("scope"));
        addScopes(scopes, claims.get("scp"));
        addScopes(scopes, claims.get("scopes"));
        return List.copyOf(scopes);
    }

    private void addRoles(Set<String> roles, Object claimValue) {
        for (String role : readStringValues(claimValue)) {
            String normalized = normalizeRole(role);
            if (StringUtils.hasText(normalized)) {
                roles.add(normalized);
            }
        }
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }

        String normalized = role.trim().toUpperCase();
        if (!normalized.startsWith(ROLE_PREFIX)) {
            normalized = ROLE_PREFIX + normalized;
        }
        return normalized;
    }

    private void addScopes(Set<String> scopes, Object claimValue) {
        for (String scope : readStringValues(claimValue)) {
            String normalized = normalizeScope(scope);
            if (StringUtils.hasText(normalized)) {
                scopes.add(normalized);
            }
        }
    }

    private String normalizeScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return null;
        }

        String normalized = scope.trim();
        if (!normalized.startsWith(SCOPE_PREFIX)) {
            normalized = SCOPE_PREFIX + normalized;
        }
        return normalized;
    }

    private List<String> readStringValues(Object claimValue) {
        if (claimValue instanceof String value) {
            return splitStringClaim(value);
        }
        if (claimValue instanceof Collection<?> values) {
            List<String> strings = new ArrayList<>();
            for (Object value : values) {
                if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
                    strings.add(stringValue);
                }
            }
            return strings;
        }
        return List.of();
    }

    private List<String> splitStringClaim(String claimValue) {
        if (!StringUtils.hasText(claimValue)) {
            return List.of();
        }
        return List.of(claimValue.trim().split("\\s+"));
    }

    private Optional<Long> readLongClaim(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            try {
                return Optional.of(Long.parseLong(stringValue.trim()));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private String readStringClaim(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof String stringValue) {
            return stringValue;
        }
        return null;
    }
}
