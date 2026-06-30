package com.seitenbau.sdc.sdc2026eventsourcing.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BankAccountReadModelRepository extends JpaRepository<BankAccountReadModel, UUID> {

    // Spring Data leitet daraus automatisch die SQL-Query ab:
    // SELECT * FROM bank_account_read_model WHERE balance > :threshold
    // Genau diese Abfrage wäre auf dem Event Store direkt nicht möglich.
    List<BankAccountReadModel> findByBalanceGreaterThan(BigDecimal threshold);
}
