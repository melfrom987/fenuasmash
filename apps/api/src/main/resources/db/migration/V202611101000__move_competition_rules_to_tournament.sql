-- Competition format and scoring are tournament rules, shared by all its events.
-- There is no production database yet: existing rows simply receive standard rules.

ALTER TABLE tournament
    ADD COLUMN competition_type VARCHAR(10) NOT NULL DEFAULT 'INDIVIDUAL',
    ADD COLUMN draw_format      VARCHAR(30) NOT NULL DEFAULT 'SINGLE_ELIMINATION',
    ADD COLUMN play_mode        VARCHAR(5)  NOT NULL DEFAULT 'SETS',
    ADD COLUMN games_to_win     INTEGER     NOT NULL DEFAULT 2,
    ADD COLUMN points_to_win    INTEGER     NOT NULL DEFAULT 21,
    ADD COLUMN two_point_lead   BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN point_cap        INTEGER              DEFAULT 30,
    ADD COLUMN relay_step       INTEGER;

ALTER TABLE tournament
    ALTER COLUMN competition_type DROP DEFAULT,
    ALTER COLUMN draw_format DROP DEFAULT,
    ALTER COLUMN play_mode DROP DEFAULT,
    ALTER COLUMN games_to_win DROP DEFAULT,
    ALTER COLUMN points_to_win DROP DEFAULT,
    ALTER COLUMN two_point_lead DROP DEFAULT,
    ALTER COLUMN point_cap DROP DEFAULT;

ALTER TABLE tournament
    ADD CONSTRAINT tournament_competition_type_check CHECK (competition_type IN ('INDIVIDUAL', 'TEAM')),
    ADD CONSTRAINT tournament_draw_format_check
        CHECK (draw_format IN ('SINGLE_ELIMINATION', 'POOLS_THEN_ELIMINATION', 'SINGLE_POOL')),
    ADD CONSTRAINT tournament_play_mode_check CHECK (play_mode IN ('SETS', 'RELAY')),
    ADD CONSTRAINT tournament_scoring_check
        CHECK (games_to_win BETWEEN 1 AND 3
            AND points_to_win BETWEEN 1 AND 50
            AND ((two_point_lead AND point_cap BETWEEN points_to_win AND 50)
                OR (NOT two_point_lead AND point_cap IS NULL))),
    ADD CONSTRAINT tournament_relay_check
        CHECK ((play_mode = 'RELAY' AND relay_step BETWEEN 1 AND 50)
            OR (play_mode = 'SETS' AND relay_step IS NULL));

CREATE TABLE tournament_discipline (
    tournament_id UUID       NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    sort_order    INTEGER    NOT NULL,
    discipline    VARCHAR(2) NOT NULL,
    PRIMARY KEY (tournament_id, sort_order),
    CONSTRAINT tournament_discipline_check CHECK (discipline IN ('MS', 'WS', 'MD', 'WD', 'XD', 'OD'))
);

INSERT INTO tournament_discipline (tournament_id, sort_order, discipline)
SELECT t.id, d.position - 1, d.code
FROM tournament t
CROSS JOIN unnest(ARRAY['MS', 'WS', 'MD', 'WD', 'XD']) WITH ORDINALITY AS d (code, position);

ALTER TABLE event
    DROP CONSTRAINT event_format_check,
    DROP CONSTRAINT event_pool_settings_check,
    DROP CONSTRAINT event_scoring_check,
    DROP CONSTRAINT event_type_check,
    DROP COLUMN format,
    DROP COLUMN games_to_win,
    DROP COLUMN points_to_win,
    DROP COLUMN point_cap,
    ADD COLUMN category VARCHAR(100),
    ADD COLUMN pool_count INTEGER;

ALTER TABLE event RENAME COLUMN type TO discipline;

UPDATE event SET category = name, pool_size = NULL, qualifiers_per_pool = NULL;

ALTER TABLE event
    ALTER COLUMN category SET NOT NULL,
    ADD CONSTRAINT event_discipline_check CHECK (discipline IN ('MS', 'WS', 'MD', 'WD', 'XD', 'OD')),
    ADD CONSTRAINT event_pool_structure_check
        CHECK ((pool_count IS NULL AND pool_size IS NULL AND qualifiers_per_pool IS NULL)
            OR (pool_count >= 1
                AND pool_size BETWEEN 3 AND 8
                AND qualifiers_per_pool BETWEEN 1 AND pool_size - 1));
