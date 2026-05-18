package com.cargo.booking.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum EquipmentType {
    TWENTY_FT("20FT"),
    FORTY_FT("40FT"),
    FORTY_HC("40HC"),
    REEFER("REEFER");

    private final String code;

    EquipmentType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static EquipmentType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Equipment type code must not be blank");
        }

        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown equipment type code: " + code));
    }
}
