package com.seitenbau.sdc.sdc2026eventsourcing.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties({"aggregateId", "occurredAt"})  // ← nicht im Payload serialisieren
public record AccountOpenedEvent(
        UUID aggregateId,
        Instant occurredAt,
        String owner,
        BigDecimal initialBalance
) implements DomainEvent {

    @JsonCreator
    public static AccountOpenedEvent fromJson(
            @JsonProperty("owner") String owner,
            @JsonProperty("initialBalance") BigDecimal initialBalance) {
        // aggregateId/occurredAt kommen aus den DB-Spalten, nicht aus dem JSON
        return new AccountOpenedEvent(null, null, owner, initialBalance);
    }

    public static AccountOpenedEvent of(UUID aggregateId, String owner, BigDecimal initialBalance) {
        return new AccountOpenedEvent(aggregateId, Instant.now(), owner, initialBalance);
    }

    public static AccountOpenedEvent reconstruct(
            UUID aggregateId,
            Instant occurredAt,
            String owner,
            BigDecimal initialBalance
    ) {
        return new AccountOpenedEvent(aggregateId, occurredAt, owner, initialBalance);
    }
}
