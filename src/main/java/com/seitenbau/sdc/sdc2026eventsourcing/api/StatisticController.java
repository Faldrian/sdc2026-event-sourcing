package com.seitenbau.sdc.sdc2026eventsourcing.api;

import com.seitenbau.sdc.sdc2026eventsourcing.readmodel.BankAccountReadModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticController {

    private final BankAccountReadModelRepository repository;

    // GET /statistics/accounts/above-balance?threshold=100
    // → Direkte SQL-Abfrage auf dem Read Model
    // → auf dem Event-Store direkt so nicht möglich
    @GetMapping("/accounts/above-balance")
    public ResponseEntity<List<AccountSummary>> accountsAboveBalance(
            @RequestParam BigDecimal threshold) {

        var result = repository.findByBalanceGreaterThan(threshold)
                .stream()
                .map(rm -> new AccountSummary(rm.getId(), rm.getOwner(), rm.getBalance()))
                .toList();

        return ResponseEntity.ok(result);
    }
}

record AccountSummary(java.util.UUID id, String owner, BigDecimal balance) {
}
