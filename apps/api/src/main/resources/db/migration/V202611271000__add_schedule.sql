-- Schedule of a tournament (#218): settings, match numbers and sources, planned times.

-- Tournament settings and state of its schedule
ALTER TABLE tournament
    ADD COLUMN average_match_minutes    INTEGER     NOT NULL DEFAULT 25,
    ADD COLUMN convocation_lead_minutes INTEGER     NOT NULL DEFAULT 30,
    ADD COLUMN public_visibility        VARCHAR(20) NOT NULL DEFAULT 'ON_PUBLISH',
    ADD COLUMN schedule_status          VARCHAR(20) NOT NULL DEFAULT 'NOT_GENERATED',
    ADD COLUMN schedule_generated_at    TIMESTAMPTZ,
    ADD COLUMN schedule_published_at    TIMESTAMPTZ,
    ADD CONSTRAINT tournament_average_match_minutes_check CHECK (average_match_minutes BETWEEN 10 AND 120),
    ADD CONSTRAINT tournament_convocation_lead_minutes_check CHECK (convocation_lead_minutes BETWEEN 0 AND 120),
    ADD CONSTRAINT tournament_public_visibility_check CHECK (public_visibility IN ('ON_PUBLISH', 'AT_START')),
    ADD CONSTRAINT tournament_schedule_status_check
        CHECK (schedule_status IN ('NOT_GENERATED', 'DRAFT', 'PUBLISHED'));

-- End of the planned slot of an event
ALTER TABLE event
    ADD COLUMN planned_end_date TIMESTAMPTZ,
    ADD CONSTRAINT event_planned_end_date_check
        CHECK (planned_end_date IS NULL OR planned_date IS NULL OR planned_end_date > planned_date);

-- Match number in the tournament, where an unknown side comes from, and its planned place
ALTER TABLE match
    ADD COLUMN number            INTEGER,
    ADD COLUMN source_a_kind     VARCHAR(10),
    ADD COLUMN source_a_pool     VARCHAR(2),
    ADD COLUMN source_a_rank     INTEGER,
    ADD COLUMN source_a_match_id UUID,
    ADD COLUMN source_b_kind     VARCHAR(10),
    ADD COLUMN source_b_pool     VARCHAR(2),
    ADD COLUMN source_b_rank     INTEGER,
    ADD COLUMN source_b_match_id UUID,
    ADD COLUMN planned_start_at  TIMESTAMPTZ,
    ADD COLUMN planned_court_id  UUID REFERENCES court (id) ON DELETE SET NULL,
    ADD CONSTRAINT match_number_check CHECK (number IS NULL OR number > 0),
    ADD CONSTRAINT match_source_a_check CHECK (
        (source_a_kind IS NULL AND source_a_pool IS NULL AND source_a_rank IS NULL AND source_a_match_id IS NULL)
        OR (source_a_kind = 'POOL_RANK' AND source_a_pool IS NOT NULL AND source_a_rank > 0
            AND source_a_match_id IS NULL)
        OR (source_a_kind IN ('WINNER', 'LOSER') AND source_a_match_id IS NOT NULL AND source_a_pool IS NULL
            AND source_a_rank IS NULL)),
    ADD CONSTRAINT match_source_b_check CHECK (
        (source_b_kind IS NULL AND source_b_pool IS NULL AND source_b_rank IS NULL AND source_b_match_id IS NULL)
        OR (source_b_kind = 'POOL_RANK' AND source_b_pool IS NOT NULL AND source_b_rank > 0
            AND source_b_match_id IS NULL)
        OR (source_b_kind IN ('WINNER', 'LOSER') AND source_b_match_id IS NOT NULL AND source_b_pool IS NULL
            AND source_b_rank IS NULL)),
    ADD CONSTRAINT match_planned_check CHECK ((planned_start_at IS NULL) = (planned_court_id IS NULL));

CREATE UNIQUE INDEX match_tournament_id_number_key ON match (tournament_id, number) WHERE number IS NOT NULL;

-- Numbers of the matches already created from a draw: pools round by round, then the
-- knockout rounds, event by event in creation order
UPDATE match m
SET number = numbered.number
FROM (SELECT id,
             ROW_NUMBER() OVER (PARTITION BY tournament_id
                 ORDER BY (pool_name IS NULL), round, pool_name, bracket_position, created_at, id) AS number
      FROM match
      WHERE pool_name IS NOT NULL OR bracket_position IS NOT NULL) numbered
WHERE m.id = numbered.id;

-- Sources of the knockout matches already created: winners of the previous round
UPDATE match m
SET source_a_kind = 'WINNER', source_a_match_id = previous.id
FROM match previous
WHERE m.bracket_position IS NOT NULL AND m.round > 1 AND m.side_a_id IS NULL
  AND previous.event_id = m.event_id AND previous.round = m.round - 1
  AND previous.bracket_position = 2 * m.bracket_position - 1;

UPDATE match m
SET source_b_kind = 'WINNER', source_b_match_id = previous.id
FROM match previous
WHERE m.bracket_position IS NOT NULL AND m.round > 1 AND m.side_b_id IS NULL
  AND previous.event_id = m.event_id AND previous.round = m.round - 1
  AND previous.bracket_position = 2 * m.bracket_position;

-- Sources of the knockout lines waiting for a pool qualifier (draw lines from 0: match k of
-- round 1 plays lines 2k-2 and 2k-1)
UPDATE match m
SET source_a_kind = 'POOL_RANK', source_a_pool = s.qualifier_pool, source_a_rank = s.qualifier_rank
FROM draw_bracket_slot s
WHERE m.bracket_position IS NOT NULL AND m.round = 1 AND m.side_a_id IS NULL AND m.source_a_kind IS NULL
  AND s.event_id = m.event_id AND s.sort_order = 2 * m.bracket_position - 2 AND s.qualifier_pool IS NOT NULL;

UPDATE match m
SET source_b_kind = 'POOL_RANK', source_b_pool = s.qualifier_pool, source_b_rank = s.qualifier_rank
FROM draw_bracket_slot s
WHERE m.bracket_position IS NOT NULL AND m.round = 1 AND m.side_b_id IS NULL AND m.source_b_kind IS NULL
  AND s.event_id = m.event_id AND s.sort_order = 2 * m.bracket_position - 1 AND s.qualifier_pool IS NOT NULL;

-- A qualifier facing a bye plays first in round 2: match k of round 2 gets side A from lines
-- 4k-4 and 4k-3, side B from lines 4k-2 and 4k-1
UPDATE match m
SET source_a_kind = 'POOL_RANK', source_a_pool = q.qualifier_pool, source_a_rank = q.qualifier_rank
FROM draw_bracket_slot q, draw_bracket_slot b
WHERE m.bracket_position IS NOT NULL AND m.round = 2 AND m.side_a_id IS NULL AND m.source_a_kind IS NULL
  AND q.event_id = m.event_id AND b.event_id = m.event_id AND q.qualifier_pool IS NOT NULL AND b.bye
  AND q.sort_order IN (4 * m.bracket_position - 4, 4 * m.bracket_position - 3)
  AND b.sort_order IN (4 * m.bracket_position - 4, 4 * m.bracket_position - 3);

UPDATE match m
SET source_b_kind = 'POOL_RANK', source_b_pool = q.qualifier_pool, source_b_rank = q.qualifier_rank
FROM draw_bracket_slot q, draw_bracket_slot b
WHERE m.bracket_position IS NOT NULL AND m.round = 2 AND m.side_b_id IS NULL AND m.source_b_kind IS NULL
  AND q.event_id = m.event_id AND b.event_id = m.event_id AND q.qualifier_pool IS NOT NULL AND b.bye
  AND q.sort_order IN (4 * m.bracket_position - 2, 4 * m.bracket_position - 1)
  AND b.sort_order IN (4 * m.bracket_position - 2, 4 * m.bracket_position - 1);

-- Order, shift and start of each event in the schedule, kept for the next computations
CREATE TABLE schedule_event_setting (
    event_id      UUID        PRIMARY KEY REFERENCES event (id) ON DELETE CASCADE,
    tournament_id UUID        NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    position      INTEGER     NOT NULL,
    offset_slots  INTEGER     NOT NULL,
    start_at      TIMESTAMPTZ,
    CONSTRAINT schedule_event_setting_offset_check CHECK (offset_slots BETWEEN -100 AND 100)
);

CREATE INDEX schedule_event_setting_tournament_id_idx ON schedule_event_setting (tournament_id);

-- Later stages of an event that cannot start before a time
CREATE TABLE schedule_stage_start (
    event_id  UUID        NOT NULL REFERENCES schedule_event_setting (event_id) ON DELETE CASCADE,
    stage     VARCHAR(10) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (event_id, stage),
    CONSTRAINT schedule_stage_start_stage_check CHECK (stage IN ('KNOCKOUT', 'QUARTER', 'SEMI', 'FINAL'))
);
