package com.seitenbau.sdc.sdc2026eventsourcing.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties({"aggregateId", "occurredAt"})
public record MoneyDepositedEvent(
        UUID aggregateId,
        Instant occurredAt,
        BigDecimal amount
) implements DomainEvent {

    @JsonCreator
    public static MoneyDepositedEvent fromJson(@JsonProperty("amount") BigDecimal amount) {
        return new MoneyDepositedEvent(null, null, amount);
    }

    public static MoneyDepositedEvent of(UUID aggregateId, BigDecimal amount) {
        return new MoneyDepositedEvent(aggregateId, Instant.now(), amount);
    }

    public static MoneyDepositedEvent reconstruct(UUID aggregateId, Instant occurredAt, BigDecimal amount) {
        return new MoneyDepositedEvent(aggregateId, occurredAt, amount);
    }
}
