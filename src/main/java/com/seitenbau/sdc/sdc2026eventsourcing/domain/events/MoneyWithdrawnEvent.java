package com.seitenbau.sdc.sdc2026eventsourcing.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MoneyWithdrawnEvent(
        UUID aggregateId,
        Instant occurredAt,
        BigDecimal amount
) implements DomainEvent {

    @JsonCreator
    public MoneyWithdrawnEvent(
            @JsonProperty("aggregateId") UUID aggregateId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("amount") BigDecimal amount) {
        this.aggregateId = aggregateId;
        this.occurredAt = occurredAt;
        this.amount = amount;
    }

    public static MoneyWithdrawnEvent of(UUID aggregateId, BigDecimal amount) {
        return new MoneyWithdrawnEvent(aggregateId, Instant.now(), amount);
    }
}

