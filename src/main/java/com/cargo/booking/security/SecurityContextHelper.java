package com.cargo.booking.security;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

public final class SecurityContextHelper {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String SCOPE_PREFIX = "SCOPE_";
    private static final Set<String> PRIVILEGED_ROLES = Set.of("ROLE_SERVICE", "ROLE_OPERATOR", "ROLE_ADMIN");

    private SecurityContextHelper() {
    }

    public static String getCurrentSubject() {
        Authentication authentication = currentAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedRequester requester) {
            return requester.subject();
        }
        return authentication.getName();
    }

    public static Optional<Long> getCurrentCustomerId() {
        Authentication authentication = currentAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedRequester requester) {
            return requester.customerIdOptional();
        }

        return readLongValue(principal, "customerId")
                .or(() -> readLongValue(principal, "customer_id"))
                .or(() -> readLongValue(authentication.getDetails(), "customerId"))
                .or(() -> readLongValue(authentication.getDetails(), "customer_id"));
    }

    public static String getCurrentUsername() {
        Authentication authentication = currentAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedRequester requester && StringUtils.hasText(requester.username())) {
            return requester.username();
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        String username = readStringValue(principal, "username")
                .or(() -> readStringValue(principal, "name"))
                .or(() -> readStringValue(authentication.getDetails(), "username"))
                .or(() -> readStringValue(authentication.getDetails(), "name"))
                .orElse(null);

        return StringUtils.hasText(username) ? username : authentication.getName();
    }

    public static List<String> getCurrentRoles() {
        Authentication authentication = currentAuthentication();
        if (authentication == null) {
            return List.of();
        }

        Set<String> roles = new LinkedHashSet<>();
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedRequester requester) {
            requester.roles().stream()
                    .map(SecurityContextHelper::normalizeRole)
                    .filter(StringUtils::hasText)
                    .forEach(roles::add);
        }

        authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(SecurityContextHelper::isRoleAuthority)
                .map(SecurityContextHelper::normalizeRole)
                .filter(StringUtils::hasText)
                .forEach(roles::add);

        return List.copyOf(roles);
    }

    public static boolean hasRole(String role) {
        String normalizedRole = normalizeRole(role);
        return StringUtils.hasText(normalizedRole) && getCurrentRoles().contains(normalizedRole);
    }

    public static boolean isOwnerOrPrivileged(Long ownerId) {
        if (ownerId == null) {
            return isPrivileged();
        }

        if (isPrivileged()) {
            return true;
        }

        return getCurrentCustomerId()
                .map(ownerId::equals)
                .orElse(false);
    }

    private static boolean isPrivileged() {
        return getCurrentRoles().stream().anyMatch(PRIVILEGED_ROLES::contains);
    }

    private static Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication;
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

    private static boolean isRoleAuthority(String authority) {
        return StringUtils.hasText(authority) && !authority.trim().toUpperCase().startsWith(SCOPE_PREFIX);
    }

    private static Optional<Long> readLongValue(Object source, String key) {
        return readValue(source, key).flatMap(SecurityContextHelper::toLong);
    }

    private static Optional<String> readStringValue(Object source, String key) {
        return readValue(source, key)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(StringUtils::hasText);
    }

    private static Optional<Object> readValue(Object source, String key) {
        if (source instanceof Map<?, ?> values) {
            return Optional.ofNullable(values.get(key));
        }
        return Optional.empty();
    }

    private static Optional<Long> toLong(Object value) {
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
}
