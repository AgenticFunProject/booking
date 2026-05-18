CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    schedule_id BIGINT NOT NULL,
    quote_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(50),
    cargo_description VARCHAR(1000) NOT NULL,
    cargo_weight_kg NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_bookings_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_bookings_cargo_weight_positive
        CHECK (cargo_weight_kg >= 0.01)
);

CREATE TABLE booking_equipment_lines (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT fk_booking_equipment_lines_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT chk_booking_equipment_lines_type
        CHECK (type IN ('TWENTY_FT', 'FORTY_FT', 'FORTY_HC', 'REEFER')),
    CONSTRAINT chk_booking_equipment_lines_quantity_positive
        CHECK (quantity >= 1)
);

CREATE TABLE booking_reference_counters (
    year INTEGER PRIMARY KEY,
    next_value BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_booking_reference_counters_next_value_positive
        CHECK (next_value >= 1)
);

CREATE UNIQUE INDEX idx_booking_reference
    ON bookings (booking_reference);

CREATE INDEX idx_booking_customer_id
    ON bookings (customer_id);

CREATE INDEX idx_booking_status
    ON bookings (status);

CREATE INDEX idx_booking_schedule_id
    ON bookings (schedule_id);

CREATE INDEX idx_booking_equipment_lines_booking_id
    ON booking_equipment_lines (booking_id);
