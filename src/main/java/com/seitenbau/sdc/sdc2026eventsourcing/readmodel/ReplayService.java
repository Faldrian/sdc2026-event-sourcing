package com.seitenbau.sdc.sdc2026eventsourcing.readmodel;

import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.AccountOpenedEvent;
import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.MoneyDepositedEvent;
import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.MoneyWithdrawnEvent;
import com.seitenbau.sdc.sdc2026eventsourcing.store.EventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReplayService {

    private final EventStore eventStore;
    private final BankAccountProjector projector;
    private final BankAccountReadModelRepository readModelRepository;

    @Transactional
    public int replay() {
        // 1. Read Model komplett leeren
        readModelRepository.deleteAll();

        // 2. Alle Events aus dem Event Store laden und der Reihe nach projizieren.
        //    project()-Aufrufe laufen in der Transaktion dieses Service –
        //    NICHT in separaten Transaktionen wie beim Live-Pfad (REQUIRES_NEW).
        //    Am Ende: ein einziger Commit für das gesamte Read Model.
        var events = eventStore.loadAll();
        events.forEach(event -> {
            switch (event) {
                case AccountOpenedEvent e -> projector.project(e);
                case MoneyDepositedEvent e -> projector.project(e);
                case MoneyWithdrawnEvent e -> projector.project(e);
            }
        });

        return events.size();
    }
}
