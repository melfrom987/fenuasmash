-- Matches of an event and their status. The sides are registrations (a player,
-- or a pair in doubles); the foreign keys will be added once registrations are
-- stored by the API.

CREATE TABLE match (
    id            UUID        PRIMARY KEY,
    tournament_id UUID        NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    event_id      UUID        NOT NULL REFERENCES event (id) ON DELETE CASCADE,
    round         INTEGER,
    side_a_id     UUID,
    side_b_id     UUID,
    status        VARCHAR(20) NOT NULL,
    winner        VARCHAR(1),
    outcome       VARCHAR(20),
    state_version INTEGER     NOT NULL,
    started_at    TIMESTAMPTZ,
    ended_at      TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT match_status_check CHECK (status IN ('SCHEDULED', 'READY', 'CALLED', 'WARMUP', 'IN_PROGRESS',
        'FINISHED', 'CANCELLED', 'WALKOVER')),
    CONSTRAINT match_winner_check CHECK (winner IN ('A', 'B')),
    CONSTRAINT match_outcome_check CHECK (outcome IN ('NORMAL', 'RETIRED', 'WALKOVER')),
    CONSTRAINT match_round_check CHECK (round > 0),
    CONSTRAINT match_sides_check CHECK (side_a_id <> side_b_id),
    -- A result exactly when the match has a winner: finished or won by forfeit
    CONSTRAINT match_result_check CHECK (
        (status IN ('FINISHED', 'WALKOVER')) = (winner IS NOT NULL)
        AND (winner IS NULL) = (outcome IS NULL)
        AND (status = 'WALKOVER') = (outcome IS NOT DISTINCT FROM 'WALKOVER')),
    CONSTRAINT match_state_version_check CHECK (state_version >= 0)
);

CREATE INDEX match_event_id_idx ON match (event_id);

CREATE INDEX match_tournament_id_status_idx ON match (tournament_id, status);
