package com.seitenbau.sdc.sdc2026eventsourcing.store;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EventStoreRepository extends JpaRepository<StoredEvent, UUID> {

    // Der häufigste Zugriffstyp: alle Events eines Aggregates in Reihenfolge
    List<StoredEvent> findByAggregateIdOrderByVersionAsc(UUID aggregateId);
}
