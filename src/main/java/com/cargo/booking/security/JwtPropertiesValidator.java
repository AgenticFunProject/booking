package com.cargo.booking.security;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtPropertiesValidator implements SmartInitializingSingleton {

    private static final int MIN_HS256_SECRET_LENGTH = 32;

    private final SecurityProperties securityProperties;

    private final JwtProperties jwtProperties;

    public JwtPropertiesValidator(SecurityProperties securityProperties, JwtProperties jwtProperties) {
        this.securityProperties = securityProperties;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!securityProperties.enabled()) {
            return;
        }

        String secret = jwtProperties.secret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "app.security.jwt.secret must be configured when app.security.enabled is true"
            );
        }
        if (secret.length() < MIN_HS256_SECRET_LENGTH) {
            throw new IllegalStateException("app.security.jwt.secret must be at least 32 characters for HS256");
        }
    }
}
