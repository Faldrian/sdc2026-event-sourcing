package com.seitenbau.sdc.sdc2026eventsourcing.store;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(
        name = "domain_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_aggregate_version",
                columnNames = {"aggregate_id", "version"}
                // → Optimistic Locking: zwei parallele Requests mit derselben Version
                //   → einer bekommt DataIntegrityViolationException
        )
)
public class StoredEvent {

    // --- Getters & Setters ---
    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    // TEXT für den Start; in Produktion JSONB für SQL-JSON-Abfragen (z.B. payload->>'owner')
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Long version;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
