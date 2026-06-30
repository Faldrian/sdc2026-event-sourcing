package com.seitenbau.sdc.sdc2026eventsourcing.api;

import java.math.BigDecimal;

// --- DTOs (je eigene Datei oder als Inner Records) ---
public record OpenAccountRequest(String owner, BigDecimal initialBalance) {}
