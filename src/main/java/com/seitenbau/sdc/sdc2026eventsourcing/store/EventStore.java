package com.seitenbau.sdc.sdc2026eventsourcing.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.AccountOpenedEvent;
import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.DomainEvent;
import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.MoneyDepositedEvent;
import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.MoneyWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventStore {

    private final EventStoreRepository repository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Persistiert die pendingEvents eines Aggregates.
     *
     * @param expectedVersion aggregate.getVersion() VOR dem Command
     *                        (-1 für brandneues Aggregate, n für bestehendes mit n+1 Events)
     *                        → erstes neues Event bekommt Version expectedVersion + 1
     */
    @Transactional
    public void save(UUID aggregateId, String aggregateType,
                     List<DomainEvent> events, long expectedVersion) {

        var toSave = new ArrayList<StoredEvent>(events.size());
        long nextVersion = expectedVersion + 1;

        for (var event : events) {
            var stored = new StoredEvent();
            stored.setId(UUID.randomUUID());
            stored.setAggregateId(aggregateId);
            stored.setAggregateType(aggregateType);
            stored.setEventType(event.getClass().getSimpleName());
            stored.setPayload(serialize(event));
            stored.setVersion(nextVersion++);
            stored.setOccurredAt(event.occurredAt());
            toSave.add(stored);
        }

        // saveAll → ein Batch-Insert, eine Transaktion
        // Bei Versionskollision: UNIQUE-Constraint → DataIntegrityViolationException
        repository.saveAll(toSave);

        // Nach dem saveAll: Events für Projektoren publizieren.
        // Durch @TransactionalEventListener(AFTER_COMMIT) im Projector
        // werden die Handler erst nach dem Commit dieser Transaktion aufgerufen.
        events.forEach(eventPublisher::publishEvent);
    }

    @Transactional(readOnly = true)
    public List<DomainEvent> load(UUID aggregateId) {
        return repository
                .findByAggregateIdOrderByVersionAsc(aggregateId)
                .stream()
                .map(this::deserialize)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Serialisierung: Domain-Objekt → JSON-String
    // -------------------------------------------------------------------------
    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Event konnte nicht serialisiert werden", e);
        }
    }

    // -------------------------------------------------------------------------
    // Deserialisierung: gespeicherter eventType-String → konkretes Event-Objekt
    // Hier kein sealed-Switch möglich, da wir auf Strings arbeiten
    // -------------------------------------------------------------------------
    private DomainEvent deserialize(StoredEvent stored) {
        try {
            return switch (stored.getEventType()) {
                case "AccountOpenedEvent" -> {
                    var e = objectMapper.readValue(stored.getPayload(), AccountOpenedEvent.class);
                    yield AccountOpenedEvent.reconstruct(
                            stored.getAggregateId(),
                            stored.getOccurredAt(),
                            e.owner(),
                            e.initialBalance());
                }
                case "MoneyDepositedEvent" -> {
                    var e = objectMapper.readValue(stored.getPayload(), MoneyDepositedEvent.class);
                    yield MoneyDepositedEvent.reconstruct(
                            stored.getAggregateId(),
                            stored.getOccurredAt(),
                            e.amount());
                }
                case "MoneyWithdrawnEvent" -> {
                    var e = objectMapper.readValue(stored.getPayload(), MoneyWithdrawnEvent.class);
                    yield MoneyWithdrawnEvent.reconstruct(
                            stored.getAggregateId(),
                            stored.getOccurredAt(),
                            e.amount());
                }
                default -> throw new IllegalArgumentException(
                        "Unbekannter Event-Typ: " + stored.getEventType());
            };
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Event konnte nicht deserialisiert werden", e);
        }
    }
}

