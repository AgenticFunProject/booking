package com.cargo.booking.config;

import com.cargo.booking.security.JwtAccessDeniedHandler;
import com.cargo.booking.security.JwtAuthenticationEntryPoint;
import com.cargo.booking.security.JwtAuthenticationFilter;
import com.cargo.booking.security.JwtProperties;
import com.cargo.booking.security.SecurityProperties;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({SecurityProperties.class, JwtProperties.class})
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final AuthenticatedRequesterMdcFilter authenticatedRequesterMdcFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            SecurityProperties securityProperties,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler,
            @Value("${app.cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins
    ) {
        this.securityProperties = securityProperties;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
        this.authenticatedRequesterMdcFilter = new AuthenticatedRequesterMdcFilter();
        this.allowedOrigins = List.copyOf(allowedOrigins);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        configureCommonStatelessApiSecurity(http);

        if (!securityProperties.enabled()) {
            return http
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                    .build();
        }

        // Rate limiting is delegated to the API gateway.
        // If standalone rate limiting is needed, consider Bucket4j or Resilience4j RateLimiter.
        return http
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/swagger-ui", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api-docs", "/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/metrics")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings")
                        .hasAnyRole("CUSTOMER", "SERVICE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings")
                        .hasAnyRole("CUSTOMER", "SERVICE", "OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*")
                        .hasAnyRole("CUSTOMER", "SERVICE", "OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/bookings/*/cancel")
                        .hasAnyRole("CUSTOMER", "SERVICE", "ADMIN")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/bookings/*/confirm",
                                "/api/v1/bookings/*/start",
                                "/api/v1/bookings/*/complete")
                        .hasAnyRole("OPERATOR", "ADMIN")
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(authenticatedRequesterMdcFilter, JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Request-ID"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Request-ID"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void configureCommonStatelessApiSecurity(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> { })
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
