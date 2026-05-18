package com.cargo.booking.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.cargo.booking.client.dto.EquipmentLineDTO;
import com.cargo.booking.client.dto.ScheduleDTO;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

class ClientStubTest {

    @Test
    void shouldProvideDeterministicScheduleSuccessData() {
        ScheduleClientStub stub = new ScheduleClientStub();

        ScheduleDTO schedule = stub.getScheduleDetails(123L);

        assertThat(stub.validateSchedule(123L)).isTrue();
        assertThat(schedule.id()).isEqualTo(123L);
        assertThat(schedule.routeName()).isEqualTo("LOCAL-STUB-ROUTE");
        assertThat(schedule.departureDate()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(schedule.status()).isEqualTo("OPEN");
    }

    @Test
    void shouldAcceptEquipmentReservationAndReleaseWithoutExternalCalls() {
        EquipmentClientStub stub = new EquipmentClientStub();

        assertThatCode(() -> stub.reserveEquipment(456L, List.of(new EquipmentLineDTO("20FT", 2))))
                .doesNotThrowAnyException();
        assertThatCode(() -> stub.releaseEquipment(456L)).doesNotThrowAnyException();
    }

    @Test
    void shouldValidateAnyQuoteForLocalDevelopment() {
        QuoteClientStub stub = new QuoteClientStub();

        assertThat(stub.validateQuote(789L, 123L, BigDecimal.valueOf(1000))).isTrue();
    }

    @Test
    void shouldBeLocalProfileServiceBeans() {
        assertLocalServiceBean(ScheduleClientStub.class);
        assertLocalServiceBean(EquipmentClientStub.class);
        assertLocalServiceBean(QuoteClientStub.class);
    }

    private static void assertLocalServiceBean(Class<?> stubType) {
        assertThat(stubType).hasAnnotation(Service.class);
        assertThat(stubType.getAnnotation(Profile.class).value()).containsExactly("local");
    }
}
