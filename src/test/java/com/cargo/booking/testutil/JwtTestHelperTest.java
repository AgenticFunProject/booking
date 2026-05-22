package com.cargo.booking.testutil;

import static org.assertj.core.api.Assertions.assertThat;

import com.cargo.booking.security.AuthenticatedRequester;
import com.cargo.booking.security.JwtTokenProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTestHelperTest {

    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(JwtTestHelper.jwtProperties());

    @Test
    void shouldGenerateGenericTokenWithRequesterClaims() {
        String token = JwtTestHelper.generateToken("requester-1", "requester.one", List.of("CUSTOMER", "SERVICE"));

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getSubjectFromToken(token)).isEqualTo("requester-1");
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("requester.one");
        assertThat(tokenProvider.getRolesFromToken(token)).containsExactly("ROLE_CUSTOMER", "ROLE_SERVICE");
    }

    @Test
    void shouldGenerateCustomerTokenWithCustomerIdClaim() {
        String token = JwtTestHelper.generateCustomerToken(3001L);

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getCustomerIdFromToken(token)).contains(3001L);
        assertThat(tokenProvider.getRolesFromToken(token)).containsExactly("ROLE_CUSTOMER");
        assertThat(tokenProvider.getAuthentication(token).getPrincipal())
                .isEqualTo(new AuthenticatedRequester(
                        "customer-3001",
                        3001L,
                        "customer-3001",
                        List.of("ROLE_CUSTOMER")
                ));
    }

    @Test
    void shouldGenerateCustomerTokenWithSnakeCaseCustomerIdClaim() {
        String token = JwtTestHelper.generateCustomerTokenWithSnakeCaseClaim(3002L);

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getCustomerIdFromToken(token)).contains(3002L);
    }

    @Test
    void shouldGenerateServiceOperatorAndAdminTokens() {
        String serviceToken = JwtTestHelper.generateServiceToken("booking-worker");
        String operatorToken = JwtTestHelper.generateOperatorToken("operator-1");
        String adminToken = JwtTestHelper.generateAdminToken();

        assertThat(tokenProvider.getRolesFromToken(serviceToken)).containsExactly("ROLE_SERVICE");
        assertThat(tokenProvider.getCustomerIdFromToken(serviceToken)).isEmpty();
        assertThat(tokenProvider.getRolesFromToken(operatorToken)).containsExactly("ROLE_OPERATOR");
        assertThat(tokenProvider.getRolesFromToken(adminToken)).containsExactly("ROLE_ADMIN");
        assertThat(tokenProvider.getSubjectFromToken(adminToken)).isEqualTo("users-42");
        assertThat(tokenProvider.getUsernameFromToken(adminToken)).isEqualTo("Users Admin");
    }

    @Test
    void shouldGenerateNegativeTokenVariants() {
        assertThat(tokenProvider.validateToken(JwtTestHelper.generateMalformedToken())).isFalse();
        assertThat(tokenProvider.validateToken(JwtTestHelper.generateExpiredToken("customer-1"))).isFalse();
        assertThat(tokenProvider.validateToken(JwtTestHelper.generateWrongIssuerToken("customer-1"))).isFalse();
        assertThat(tokenProvider.validateToken(JwtTestHelper.generateWrongAudienceToken("customer-1"))).isFalse();
        assertThat(tokenProvider.validateToken(JwtTestHelper.generateInvalidSignatureToken("customer-1"))).isFalse();
        assertThat(tokenProvider.validateToken(JwtTestHelper.generateMissingSubjectToken())).isFalse();
    }

    @Test
    void shouldGenerateCustomerTokenMissingOwnershipClaim() {
        String token = JwtTestHelper.generateCustomerTokenMissingCustomerClaim("customer-1");

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getRolesFromToken(token)).containsExactly("ROLE_CUSTOMER");
        assertThat(tokenProvider.getCustomerIdFromToken(token)).isEmpty();
    }
}
