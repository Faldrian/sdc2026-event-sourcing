package com.seitenbau.sdc.sdc2026eventsourcing.domain.events;

import java.time.Instant;
import java.util.UUID;

// sealed: Compiler prüft Vollständigkeit im switch – kein default nötig
public sealed interface DomainEvent
        permits AccountOpenedEvent, MoneyDepositedEvent, MoneyWithdrawnEvent {

    UUID aggregateId();

    Instant occurredAt();
}
