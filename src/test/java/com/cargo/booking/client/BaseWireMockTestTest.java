package com.cargo.booking.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.cargo.booking.config.IntegrationProperties;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "local"})
class BaseWireMockTestTest extends BaseWireMockTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private IntegrationProperties integrationProperties;

    @Test
    void shouldInjectEmbeddedPostgresConnectionDetails() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:postgresql://localhost:");
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    void shouldInjectWireMockBaseUrlsForExternalServices() {
        assertThat(scheduleApi().isRunning()).isTrue();
        assertThat(equipmentApi().isRunning()).isTrue();
        assertThat(quoteApi().isRunning()).isTrue();

        assertThat(integrationProperties.scheduleApi().baseUrl()).isEqualTo(scheduleApi().baseUrl());
        assertThat(integrationProperties.equipmentApi().baseUrl()).isEqualTo(equipmentApi().baseUrl());
        assertThat(integrationProperties.quoteApi().baseUrl()).isEqualTo(quoteApi().baseUrl());
        assertThat(integrationProperties.scheduleApi().baseUrl())
                .isNotEqualTo(integrationProperties.equipmentApi().baseUrl())
                .isNotEqualTo(integrationProperties.quoteApi().baseUrl());
    }
}
