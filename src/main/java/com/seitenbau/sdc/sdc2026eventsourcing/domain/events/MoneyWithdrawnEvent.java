package com.seitenbau.sdc.sdc2026eventsourcing.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties({"aggregateId", "occurredAt"})
public record MoneyWithdrawnEvent(
        UUID aggregateId,
        Instant occurredAt,
        BigDecimal amount
) implements DomainEvent {

    @JsonCreator
    public static MoneyWithdrawnEvent fromJson(@JsonProperty("amount") BigDecimal amount) {
        return new MoneyWithdrawnEvent(null, null, amount);
    }

    public static MoneyWithdrawnEvent of(UUID aggregateId, BigDecimal amount) {
        return new MoneyWithdrawnEvent(aggregateId, Instant.now(), amount);
    }

    public static MoneyWithdrawnEvent reconstruct(UUID aggregateId, Instant occurredAt, BigDecimal amount) {
        return new MoneyWithdrawnEvent(aggregateId, occurredAt, amount);
    }
}
