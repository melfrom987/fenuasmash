-- Draw of an event (#147): its pools and the first round of its knockout bracket. One draw per
-- event, first a draft, then published (which locks the registrations of the event).
CREATE TABLE draw (
    event_id                UUID PRIMARY KEY REFERENCES event (id) ON DELETE CASCADE,
    status                  VARCHAR(10) NOT NULL,
    seed_count              INTEGER     NOT NULL,
    random_seed             BIGINT      NOT NULL,
    requested_pool_count    INTEGER,
    requested_qualifiers    INTEGER,
    requested_seed_count    INTEGER,
    bracket_size            INTEGER,
    created_at              TIMESTAMPTZ NOT NULL,
    published_at            TIMESTAMPTZ,
    CONSTRAINT draw_status_check CHECK (status IN ('DRAFT', 'PUBLISHED'))
);

CREATE TABLE draw_pool (
    event_id   UUID       NOT NULL REFERENCES draw (event_id) ON DELETE CASCADE,
    sort_order INTEGER    NOT NULL,
    name       VARCHAR(2) NOT NULL,
    qualifiers INTEGER    NOT NULL,
    PRIMARY KEY (event_id, sort_order)
);

CREATE TABLE draw_pool_entry (
    event_id        UUID       NOT NULL REFERENCES draw (event_id) ON DELETE CASCADE,
    sort_order      INTEGER    NOT NULL,
    pool_name       VARCHAR(2) NOT NULL,
    registration_id UUID       NOT NULL,
    seed            INTEGER,
    PRIMARY KEY (event_id, sort_order)
);

CREATE TABLE draw_bracket_slot (
    event_id        UUID    NOT NULL REFERENCES draw (event_id) ON DELETE CASCADE,
    sort_order      INTEGER NOT NULL,
    registration_id UUID,
    seed            INTEGER,
    qualifier_pool  VARCHAR(2),
    qualifier_rank  INTEGER,
    bye             BOOLEAN NOT NULL,
    PRIMARY KEY (event_id, sort_order)
);
