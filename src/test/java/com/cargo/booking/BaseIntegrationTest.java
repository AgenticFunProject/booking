package com.cargo.booking;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
public abstract class BaseIntegrationTest {

    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_DATABASE = "postgres";
    private static final EmbeddedPostgres EMBEDDED_POSTGRES = startEmbeddedPostgres();

    @DynamicPropertySource
    static void embeddedPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> EMBEDDED_POSTGRES.getJdbcUrl(POSTGRES_USER, POSTGRES_DATABASE)
        );
        registry.add("spring.datasource.username", () -> POSTGRES_USER);
        registry.add("spring.datasource.password", () -> POSTGRES_USER);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    protected static EmbeddedPostgres embeddedPostgres() {
        return EMBEDDED_POSTGRES;
    }

    private static EmbeddedPostgres startEmbeddedPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
