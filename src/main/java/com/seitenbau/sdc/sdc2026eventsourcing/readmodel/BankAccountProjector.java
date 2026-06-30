package com.seitenbau.sdc.sdc2026eventsourcing.readmodel;

import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.AccountOpenedEvent;
import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.MoneyDepositedEvent;
import com.seitenbau.sdc.sdc2026eventsourcing.domain.events.MoneyWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BankAccountProjector {

    private final BankAccountReadModelRepository repository;

    // -----------------------------------------------------------------------
    // Live-Pfad: wird nach jedem Commit des Event-Stores aufgerufen
    //
    // @TransactionalEventListener(AFTER_COMMIT):
    //   → wird erst aufgerufen, NACHDEM der Event-Store committed hat
    //   → Read Model wird in einer EIGENEN, neuen Transaktion geschrieben
    //   → zeigt "Eventual Consistency": kurzes Fenster zwischen Write- und Read-Model
    //
    // @Transactional(REQUIRES_NEW):
    //   → startet eine neue Transaktion für das Read-Model-Update
    //   → nötig, weil nach AFTER_COMMIT keine aktive Transaktion mehr existiert
    // -----------------------------------------------------------------------
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AccountOpenedEvent event) {
        project(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(MoneyDepositedEvent event) {
        project(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(MoneyWithdrawnEvent event) {
        project(event);
    }

    // -----------------------------------------------------------------------
    // Replay-Pfad: wird direkt vom ReplayService aufgerufen
    // package-private → keine öffentliche API, nur für ReplayService sichtbar
    // -----------------------------------------------------------------------
    void project(AccountOpenedEvent event) {
        var readModel = new BankAccountReadModel();
        readModel.setId(event.aggregateId());
        readModel.setOwner(event.owner());
        readModel.setBalance(event.initialBalance());
        readModel.setOpen(true);
        repository.save(readModel);
    }

    void project(MoneyDepositedEvent event) {
        repository.findById(event.aggregateId()).ifPresent(account -> {
            account.setBalance(account.getBalance().add(event.amount()));
            repository.save(account);
        });
    }

    void project(MoneyWithdrawnEvent event) {
        repository.findById(event.aggregateId()).ifPresent(account -> {
            account.setBalance(account.getBalance().subtract(event.amount()));
            repository.save(account);
        });
    }
}
