-- src/main/resources/db/migration/V1__create_domain_events.sql

CREATE TABLE domain_events (
                               id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                               aggregate_id   UUID         NOT NULL,
                               aggregate_type VARCHAR(255) NOT NULL,
                               event_type     VARCHAR(255) NOT NULL,
                               payload        TEXT         NOT NULL,  -- Produktion: JSONB für payload->>'field'-Queries
                               version        BIGINT       NOT NULL,
                               occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- Optimistic Locking: zwei parallele Writes mit gleicher Version → einer schlägt fehl
                               CONSTRAINT uq_aggregate_version UNIQUE (aggregate_id, version)
);

-- Pflichtindex: "gib mir alle Events für Aggregate X" ist der häufigste Query
CREATE INDEX idx_domain_events_aggregate_id ON domain_events (aggregate_id);
