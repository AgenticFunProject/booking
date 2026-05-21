package com.cargo.booking.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextHelperTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnEmptyValuesWhenUnauthenticated() {
        assertThat(SecurityContextHelper.getCurrentSubject()).isNull();
        assertThat(SecurityContextHelper.getCurrentCustomerId()).isEmpty();
        assertThat(SecurityContextHelper.getCurrentUsername()).isNull();
        assertThat(SecurityContextHelper.getCurrentRoles()).isEmpty();
        assertThat(SecurityContextHelper.hasRole("ADMIN")).isFalse();
        assertThat(SecurityContextHelper.isOwnerOrPrivileged(1001L)).isFalse();
    }

    @Test
    void shouldExposeCurrentRequesterClaimsFromSecurityContext() {
        AuthenticatedRequester requester = new AuthenticatedRequester(
                "user-123",
                1001L,
                "customer.one",
                List.of("ROLE_CUSTOMER")
        );
        setAuthentication(new UsernamePasswordAuthenticationToken(
                requester,
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                        new SimpleGrantedAuthority("SCOPE_booking:read")
                )
        ));

        assertThat(SecurityContextHelper.getCurrentSubject()).isEqualTo("user-123");
        assertThat(SecurityContextHelper.getCurrentCustomerId()).contains(1001L);
        assertThat(SecurityContextHelper.getCurrentUsername()).isEqualTo("customer.one");
        assertThat(SecurityContextHelper.getCurrentRoles()).containsExactly("ROLE_CUSTOMER");
        assertThat(SecurityContextHelper.hasRole("CUSTOMER")).isTrue();
        assertThat(SecurityContextHelper.hasRole("ROLE_CUSTOMER")).isTrue();
        assertThat(SecurityContextHelper.isOwnerOrPrivileged(1001L)).isTrue();
        assertThat(SecurityContextHelper.isOwnerOrPrivileged(1002L)).isFalse();
    }

    @Test
    void shouldTreatServiceOperatorAndAdminAsPrivileged() {
        for (String role : List.of("ROLE_SERVICE", "ROLE_OPERATOR", "ROLE_ADMIN")) {
            SecurityContextHolder.clearContext();
            setAuthentication(new UsernamePasswordAuthenticationToken(
                    new AuthenticatedRequester("system", null, "system", List.of(role)),
                    null,
                    List.of(new SimpleGrantedAuthority(role))
            ));

            assertThat(SecurityContextHelper.isOwnerOrPrivileged(2002L)).isTrue();
        }
    }

    @Test
    void shouldNormalizeUsersAdminRoleShape() {
        setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedRequester("admin-user", null, "Admin User", List.of("admin")),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));

        assertThat(SecurityContextHelper.getCurrentRoles()).containsExactly("ROLE_ADMIN");
        assertThat(SecurityContextHelper.hasRole("ADMIN")).isTrue();
        assertThat(SecurityContextHelper.isOwnerOrPrivileged(9999L)).isTrue();
    }

    @Test
    void shouldReadCustomerAndUsernameFromMapDetailsForFutureTokenShapes() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "service-subject",
                null,
                "ROLE_CUSTOMER"
        );
        authentication.setDetails(Map.of(
                "customer_id", "3003",
                "name", "Customer Three"
        ));
        setAuthentication(authentication);

        assertThat(SecurityContextHelper.getCurrentSubject()).isEqualTo("service-subject");
        assertThat(SecurityContextHelper.getCurrentCustomerId()).contains(3003L);
        assertThat(SecurityContextHelper.getCurrentUsername()).isEqualTo("Customer Three");
        assertThat(SecurityContextHelper.getCurrentRoles()).containsExactly("ROLE_CUSTOMER");
    }

    private void setAuthentication(org.springframework.security.core.Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
