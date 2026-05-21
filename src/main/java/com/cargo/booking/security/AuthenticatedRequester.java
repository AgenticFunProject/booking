package com.cargo.booking.security;

import java.util.List;
import java.util.Optional;
import org.springframework.security.core.AuthenticatedPrincipal;

public record AuthenticatedRequester(
        String subject,
        Long customerId,
        String username,
        List<String> roles
) implements AuthenticatedPrincipal {

    public AuthenticatedRequester {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    @Override
    public String getName() {
        return subject;
    }

    public Optional<Long> customerIdOptional() {
        return Optional.ofNullable(customerId);
    }
}
