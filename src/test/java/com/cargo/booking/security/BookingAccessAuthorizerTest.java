package com.cargo.booking.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cargo.booking.exception.BookingValidationException;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.repository.BookingRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class BookingAccessAuthorizerTest {

    @Mock
    private BookingRepository bookingRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class SecurityDisabled {

        @Test
        void shouldAllowCreateWithoutOwnershipCheckWhenSecurityIsDisabled() {
            BookingAccessAuthorizer authorizer = disabledAuthorizer();

            assertThatCode(() -> authorizer.authorizeCreateCustomer(1001L)).doesNotThrowAnyException();

            verifyNoInteractions(bookingRepository);
        }

        @Test
        void shouldAllowBookingAccessWithoutRepositoryLookupWhenSecurityIsDisabled() {
            BookingAccessAuthorizer authorizer = disabledAuthorizer();

            assertThatCode(() -> authorizer.authorizeBookingAccess(42L)).doesNotThrowAnyException();
            assertThatCode(() -> authorizer.authorizeBookingAccess("BKG-2026-00042")).doesNotThrowAnyException();

            verifyNoInteractions(bookingRepository);
        }
    }

    @Nested
    class PrivilegedCallers {

        @Test
        void shouldAllowServiceOperatorAndAdminWithoutCustomerOwnershipChecks() {
            for (String role : List.of("ROLE_SERVICE", "ROLE_OPERATOR", "ROLE_ADMIN")) {
                SecurityContextHolder.clearContext();
                setRequester(role, null);
                BookingAccessAuthorizer authorizer = enabledAuthorizer();

                assertThatCode(() -> authorizer.authorizeCreateCustomer(9999L)).doesNotThrowAnyException();
                assertThatCode(() -> authorizer.authorizeListCustomer(null)).doesNotThrowAnyException();
                assertThatCode(() -> authorizer.authorizeBookingAccess(42L)).doesNotThrowAnyException();
                assertThatCode(() -> authorizer.authorizeBookingAccess("BKG-2026-00042")).doesNotThrowAnyException();
            }

            verifyNoInteractions(bookingRepository);
        }

        @Test
        void shouldTreatUsersAdminRoleShapeAsPrivileged() {
            setRequester("admin", null);
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatCode(() -> authorizer.authorizeCreateCustomer(9999L)).doesNotThrowAnyException();
            assertThatCode(() -> authorizer.authorizeBookingAccess(42L)).doesNotThrowAnyException();

            verifyNoInteractions(bookingRepository);
        }
    }

    @Nested
    class CustomerRequestChecks {

        @Test
        void shouldAllowCreateWhenRequestCustomerMatchesTokenCustomer() {
            setRequester("ROLE_CUSTOMER", 3001L);
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatCode(() -> authorizer.authorizeCreateCustomer(3001L)).doesNotThrowAnyException();

            verifyNoInteractions(bookingRepository);
        }

        @Test
        void shouldRejectCreateWhenRequestCustomerDoesNotMatchTokenCustomer() {
            setRequester("ROLE_CUSTOMER", 3001L);
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatThrownBy(() -> authorizer.authorizeCreateCustomer(3002L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Customer ownership check failed");

            verifyNoInteractions(bookingRepository);
        }

        @Test
        void shouldRejectCreateBeforeComparingWhenCustomerClaimIsMissing() {
            setRequester("ROLE_CUSTOMER", null);
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatThrownBy(() -> authorizer.authorizeCreateCustomer(3001L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Customer identity claim is required");

            verifyNoInteractions(bookingRepository);
        }

        @Test
        void shouldRejectListWhenCustomerTokenOmitsCustomerIdQueryParameter() {
            setRequester("ROLE_CUSTOMER", 3001L);
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatThrownBy(() -> authorizer.authorizeListCustomer(null))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessage("customerId query parameter is required for customer callers");

            verifyNoInteractions(bookingRepository);
        }

        @Test
        void shouldRejectListBeforeCheckingQueryValueWhenCustomerClaimIsMissing() {
            setRequester("ROLE_CUSTOMER", null);
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatThrownBy(() -> authorizer.authorizeListCustomer(null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Customer identity claim is required");

            verifyNoInteractions(bookingRepository);
        }
    }

    @Nested
    class ExistingBookingChecks {

        @Test
        void shouldAllowBookingIdAccessWhenBookingOwnerMatchesTokenCustomer() {
            setRequester("ROLE_CUSTOMER", 3001L);
            when(bookingRepository.findById(42L)).thenReturn(Optional.of(bookingWithCustomer(3001L)));
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatCode(() -> authorizer.authorizeBookingAccess(42L)).doesNotThrowAnyException();

            verify(bookingRepository).findById(42L);
        }

        @Test
        void shouldRejectBookingIdAccessWhenBookingOwnerDoesNotMatchTokenCustomer() {
            setRequester("ROLE_CUSTOMER", 3001L);
            when(bookingRepository.findById(42L)).thenReturn(Optional.of(bookingWithCustomer(3002L)));
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatThrownBy(() -> authorizer.authorizeBookingAccess(42L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Customer ownership check failed");

            verify(bookingRepository).findById(42L);
        }

        @Test
        void shouldReturnWithoutThrowingWhenBookingIdLookupIsEmpty() {
            setRequester("ROLE_CUSTOMER", 3001L);
            when(bookingRepository.findById(42L)).thenReturn(Optional.empty());
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatCode(() -> authorizer.authorizeBookingAccess(42L)).doesNotThrowAnyException();

            verify(bookingRepository).findById(42L);
        }

        @Test
        void shouldRejectBookingIdAccessBeforeRepositoryLookupWhenCustomerClaimIsMissing() {
            setRequester("ROLE_CUSTOMER", null);
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatThrownBy(() -> authorizer.authorizeBookingAccess(42L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Customer identity claim is required");

            verify(bookingRepository, never()).findById(42L);
        }

        @Test
        void shouldAllowReferenceAccessWhenBookingOwnerMatchesTokenCustomer() {
            setRequester("ROLE_CUSTOMER", 3001L);
            when(bookingRepository.findByBookingReference("BKG-2026-00042"))
                    .thenReturn(Optional.of(bookingWithCustomer(3001L)));
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatCode(() -> authorizer.authorizeBookingAccess("BKG-2026-00042")).doesNotThrowAnyException();

            verify(bookingRepository).findByBookingReference("BKG-2026-00042");
        }

        @Test
        void shouldRejectReferenceAccessWhenBookingOwnerDoesNotMatchTokenCustomer() {
            setRequester("ROLE_CUSTOMER", 3001L);
            when(bookingRepository.findByBookingReference("BKG-2026-00042"))
                    .thenReturn(Optional.of(bookingWithCustomer(3002L)));
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatThrownBy(() -> authorizer.authorizeBookingAccess("BKG-2026-00042"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Customer ownership check failed");

            verify(bookingRepository).findByBookingReference("BKG-2026-00042");
        }

        @Test
        void shouldReturnWithoutThrowingWhenReferenceLookupIsEmpty() {
            setRequester("ROLE_CUSTOMER", 3001L);
            when(bookingRepository.findByBookingReference("BKG-2026-00042")).thenReturn(Optional.empty());
            BookingAccessAuthorizer authorizer = enabledAuthorizer();

            assertThatCode(() -> authorizer.authorizeBookingAccess("BKG-2026-00042")).doesNotThrowAnyException();

            verify(bookingRepository).findByBookingReference("BKG-2026-00042");
        }
    }

    private BookingAccessAuthorizer enabledAuthorizer() {
        return new BookingAccessAuthorizer(bookingRepository, new SecurityProperties(true));
    }

    private BookingAccessAuthorizer disabledAuthorizer() {
        return new BookingAccessAuthorizer(bookingRepository, new SecurityProperties(false));
    }

    private void setRequester(String role, Long customerId) {
        AuthenticatedRequester requester = new AuthenticatedRequester(
                "requester-subject",
                customerId,
                "requester",
                List.of(role)
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                requester,
                null,
                List.of(new SimpleGrantedAuthority(normalizeRole(role)))
        ));
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private Booking bookingWithCustomer(Long customerId) {
        return Booking.builder()
                .id(42L)
                .bookingReference("BKG-2026-00042")
                .customerId(customerId)
                .build();
    }
}
