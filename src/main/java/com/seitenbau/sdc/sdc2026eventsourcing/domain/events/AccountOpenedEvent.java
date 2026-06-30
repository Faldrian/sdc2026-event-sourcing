package com.seitenbau.sdc.sdc2026eventsourcing.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountOpenedEvent(
        UUID aggregateId,
        Instant occurredAt,
        String owner,
        BigDecimal initialBalance
) implements DomainEvent {

    // @JsonCreator + @JsonProperty → Jackson kann das Event ohne no-arg-Konstruktor deserialisieren
    @JsonCreator
    public AccountOpenedEvent(
            @JsonProperty("aggregateId") UUID aggregateId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("owner") String owner,
            @JsonProperty("initialBalance") BigDecimal initialBalance) {
        this.aggregateId = aggregateId;
        this.occurredAt = occurredAt;
        this.owner = owner;
        this.initialBalance = initialBalance;
    }

    /**
     * Factory-Methode – wird im Aggregate aufgerufen
     */
    public static AccountOpenedEvent of(UUID aggregateId, String owner, BigDecimal initialBalance) {
        return new AccountOpenedEvent(aggregateId, Instant.now(), owner, initialBalance);
    }
}
