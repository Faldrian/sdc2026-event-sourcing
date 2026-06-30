package com.seitenbau.sdc.sdc2026eventsourcing.api;

import com.seitenbau.sdc.sdc2026eventsourcing.domain.BankAccountAggregate;
import com.seitenbau.sdc.sdc2026eventsourcing.store.EventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private static final String AGGREGATE_TYPE = "BankAccount";
    private final EventStore eventStore;

    /** POST /accounts  →  neues Konto anlegen */
    @PostMapping
    public ResponseEntity<UUID> openAccount(@RequestBody OpenAccountRequest request) {
        UUID accountId = UUID.randomUUID();

        var account = BankAccountAggregate.openNewAccount(
                accountId, request.owner(), request.initialBalance());

        // account.getVersion() == -1  →  EventStore speichert erstes Event mit Version 0
        eventStore.save(accountId, AGGREGATE_TYPE,
                account.getPendingEvents(), account.getVersion());

        return ResponseEntity.status(HttpStatus.CREATED).body(accountId);
    }

    /** POST /accounts/{id}/deposit  →  Geld einzahlen */
    @PostMapping("/{id}/deposit")
    public ResponseEntity<Void> deposit(
            @PathVariable UUID id,
            @RequestBody AmountRequest request) {

        var account = loadAggregate(id);
        // account.getVersion() == 2 (wenn 3 Events existieren: 0,1,2)
        account.deposit(request.amount());
        // Nach deposit: version noch 2 (pending Events nicht gezählt)
        // → EventStore speichert nächstes Event mit Version 3
        eventStore.save(id, AGGREGATE_TYPE,
                account.getPendingEvents(), account.getVersion());

        return ResponseEntity.ok().build();
    }

    /** POST /accounts/{id}/withdraw  →  Geld auszahlen */
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Void> withdraw(
            @PathVariable UUID id,
            @RequestBody AmountRequest request) {

        var account = loadAggregate(id);
        account.withdraw(request.amount());
        eventStore.save(id, AGGREGATE_TYPE,
                account.getPendingEvents(), account.getVersion());

        return ResponseEntity.ok().build();
    }

    /** GET /accounts/{id}  →  aktueller Zustand (aus Events rekonstruiert) */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        var account = loadAggregate(id);
        return ResponseEntity.ok(
                new AccountResponse(account.getId(), account.getOwner(), account.getBalance()));
    }

    private BankAccountAggregate loadAggregate(UUID id) {
        return BankAccountAggregate.reconstitute(eventStore.load(id));
    }
}

