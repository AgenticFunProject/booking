package com.cargo.booking.model.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EquipmentTypeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldExposeExternalApiCodesAsJsonValues() throws Exception {
        assertThat(objectMapper.writeValueAsString(EquipmentType.TWENTY_FT)).isEqualTo("\"20FT\"");
        assertThat(objectMapper.writeValueAsString(EquipmentType.FORTY_FT)).isEqualTo("\"40FT\"");
        assertThat(objectMapper.writeValueAsString(EquipmentType.FORTY_HC)).isEqualTo("\"40HC\"");
        assertThat(objectMapper.writeValueAsString(EquipmentType.REEFER)).isEqualTo("\"REEFER\"");
    }

    @Test
    void shouldParseExternalApiCodes() {
        assertThat(EquipmentType.fromCode("20FT")).isEqualTo(EquipmentType.TWENTY_FT);
        assertThat(EquipmentType.fromCode("40FT")).isEqualTo(EquipmentType.FORTY_FT);
        assertThat(EquipmentType.fromCode("40HC")).isEqualTo(EquipmentType.FORTY_HC);
        assertThat(EquipmentType.fromCode("REEFER")).isEqualTo(EquipmentType.REEFER);
    }

    @Test
    void shouldParseCodesCaseInsensitivelyAndTrimWhitespace() {
        assertThat(EquipmentType.fromCode("  20ft  ")).isEqualTo(EquipmentType.TWENTY_FT);
        assertThat(EquipmentType.fromCode("reefer")).isEqualTo(EquipmentType.REEFER);
    }

    @Test
    void shouldRejectBlankEquipmentCode() {
        assertThatThrownBy(() -> EquipmentType.fromCode(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Equipment type code must not be blank");
    }

    @Test
    void shouldRejectUnknownEquipmentCode() {
        assertThatThrownBy(() -> EquipmentType.fromCode("45FT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown equipment type code: 45FT");
    }
}
