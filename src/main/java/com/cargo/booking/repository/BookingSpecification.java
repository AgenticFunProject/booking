package com.cargo.booking.repository;

import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class BookingSpecification {

    private BookingSpecification() {
    }

    public static Specification<Booking> hasCustomerId(Long customerId) {
        return customerId == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("customerId"), customerId);
    }

    public static Specification<Booking> hasStatus(BookingStatus status) {
        return status == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Booking> hasScheduleId(Long scheduleId) {
        return scheduleId == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("scheduleId"), scheduleId);
    }

    public static Specification<Booking> createdAfter(Instant from) {
        return from == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from);
    }

    public static Specification<Booking> createdBefore(Instant to) {
        return to == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.<Instant>get("createdAt"), to);
    }
}
