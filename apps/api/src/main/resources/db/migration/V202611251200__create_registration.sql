-- Registrations of players (and pairs) in events (#88). A player is registered at most once
-- per event, as the player or as the partner: the application checks the partner.
CREATE TABLE registration (
    id            UUID PRIMARY KEY,
    tournament_id UUID        NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    event_id      UUID        NOT NULL REFERENCES event (id) ON DELETE CASCADE,
    player_id     UUID        NOT NULL REFERENCES player (id),
    partner_id    UUID REFERENCES player (id),
    status        VARCHAR(20) NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT registration_status_check
        CHECK (status IN ('CONFIRMED', 'PENDING_PARTNER', 'LOOKING_FOR_PARTNER')),
    CONSTRAINT registration_partner_check CHECK (partner_id IS NULL OR partner_id <> player_id),
    CONSTRAINT registration_event_id_player_id_key UNIQUE (event_id, player_id)
);

CREATE INDEX registration_tournament_id_idx ON registration (tournament_id);
CREATE INDEX registration_partner_id_idx ON registration (partner_id);
