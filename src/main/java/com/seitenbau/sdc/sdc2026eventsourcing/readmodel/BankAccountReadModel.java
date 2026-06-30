package com.seitenbau.sdc.sdc2026eventsourcing.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

// Eigene Tabelle, eigene JPA-Entity – vollständig unabhängig vom Event Store.
// Kann jederzeit gelöscht und aus dem Event Store neu aufgebaut werden (Replay).
@Setter
@Getter
@Entity
@Table(name = "bank_account_read_model")
public class BankAccountReadModel {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private boolean open;

}
