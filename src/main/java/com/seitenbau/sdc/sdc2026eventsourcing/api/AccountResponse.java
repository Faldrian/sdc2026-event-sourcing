package com.seitenbau.sdc.sdc2026eventsourcing.api;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID id, String owner, BigDecimal balance) {}
