package com.seitenbau.sdc.sdc2026eventsourcing.api;

import com.seitenbau.sdc.sdc2026eventsourcing.readmodel.BankAccountReadModelRepository;
import com.seitenbau.sdc.sdc2026eventsourcing.readmodel.ReplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ReplayController {

    private final ReplayService replayService;
    private final BankAccountReadModelRepository readModelRepository;

    // Löscht nur das Read Model – Event Store bleibt unangetastet.
    // Zeigt im Vortrag: "Das Read Model ist weg – aber die Events sind noch da."
    @DeleteMapping("/read-model")
    public ResponseEntity<Void> clearReadModel() {
        readModelRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }

    // Baut das Read Model vollständig neu aus dem Event Store auf.
    // Zeigt im Vortrag: "Replay – und alles ist wieder da."
    @PostMapping("/read-model/replay")
    public ResponseEntity<ReplayResult> replay() {
        int eventsReplayed = replayService.replay();
        return ResponseEntity.ok(new ReplayResult(eventsReplayed));
    }
}

