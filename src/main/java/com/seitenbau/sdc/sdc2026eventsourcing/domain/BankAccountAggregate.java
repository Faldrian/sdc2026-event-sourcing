package com.seitenbau.sdc.sdc2026eventsourcing.domain;

import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.AccountOpenedEvent;
import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.DomainEvent;
import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.MoneyDepositedEvent;
import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.MoneyWithdrawnEvent;
import com.seitenbau.sdc.sdc2026eventsourcing.exception.AggregateNotFoundException;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.*;

public class BankAccountAggregate {

    // ----- State -----
    @Getter
    private UUID id;

    @Getter
    private String owner;

    @Getter
    private BigDecimal balance;

    @Getter
    private boolean open;

    /**
     * Version des zuletzt PERSISTIERTEN Events.
     *   -1  → noch kein Event je persistiert (neues Aggregate)
     *    0  → erstes Event (Version 0) ist persistiert
     *    n  → n+1 Events sind persistiert
     *
     * Wird nur in reconstitute() hochgezählt, NICHT in raise().
     * Dadurch weiß der EventStore beim Speichern, ab welcher Version neue Events beginnen.
     */
    @Getter
    private long version = -1L;

    /** Events, die noch NICHT in der DB stehen */
    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    private BankAccountAggregate() {}

    // -------------------------------------------------------------------------
    // Factory: neues Konto – erzeugt einen AccountOpenedEvent
    // -------------------------------------------------------------------------
    public static BankAccountAggregate openNewAccount(
            UUID id, String owner, BigDecimal initialBalance) {

        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Startguthaben darf nicht negativ sein");
        }
        var aggregate = new BankAccountAggregate();
        aggregate.raise(AccountOpenedEvent.of(id, owner, initialBalance));
        return aggregate;
        // version bleibt -1 → EventStore speichert mit Version 0
    }

    // -------------------------------------------------------------------------
    // Reconstitution: Aggregate aus persistierten Events wiederherstellen
    // -------------------------------------------------------------------------
    public static BankAccountAggregate reconstitute(List<DomainEvent> events) {
        if (events.isEmpty()) {
            throw new AggregateNotFoundException("Kein Aggregate für diese ID gefunden");
        }
        var aggregate = new BankAccountAggregate();
        for (var event : events) {
            aggregate.apply(event);  // State aktualisieren
            aggregate.version++;     // version = Index des letzten persistierten Events
        }
        // Nach 3 Events (Versionen 0,1,2): aggregate.version = 2
        return aggregate;
    }

    // -------------------------------------------------------------------------
    // Commands – Businesslogik prüfen, dann Event erzeugen
    // -------------------------------------------------------------------------
    public void deposit(BigDecimal amount) {
        requireOpen();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Einzahlungsbetrag muss positiv sein");
        }
        raise(MoneyDepositedEvent.of(id, amount));
    }

    public void withdraw(BigDecimal amount) {
        requireOpen();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Auszahlungsbetrag muss positiv sein");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Nicht genug Guthaben");
        }
        raise(MoneyWithdrawnEvent.of(id, amount));
    }

    // -------------------------------------------------------------------------
    // raise: Event sofort auf den State anwenden + in pendingEvents merken
    // NICHT version++ hier → version repräsentiert nur Persistiertes
    // -------------------------------------------------------------------------
    private void raise(DomainEvent event) {
        apply(event);
        pendingEvents.add(event);
    }

    // -------------------------------------------------------------------------
    // apply: reine State-Mutation durch ein Event
    // sealed interface → Compiler garantiert Vollständigkeit, kein default nötig
    // -------------------------------------------------------------------------
    private void apply(DomainEvent event) {
        switch (event) {
            case AccountOpenedEvent e -> {
                this.id      = e.aggregateId();
                this.owner   = e.owner();
                this.balance = e.initialBalance();
                this.open    = true;
            }
            case MoneyDepositedEvent e -> this.balance = this.balance.add(e.amount());
            case MoneyWithdrawnEvent e -> this.balance = this.balance.subtract(e.amount());
        }
    }

    private void requireOpen() {
        if (!open) throw new IllegalStateException("Konto ist nicht aktiv");
    }

    public List<DomainEvent> getPendingEvents() { return Collections.unmodifiableList(pendingEvents); }
}
