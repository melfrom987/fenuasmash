ALTER TABLE event
    ADD COLUMN format              VARCHAR(30) NOT NULL DEFAULT 'SINGLE_ELIMINATION',
    ADD COLUMN pool_size           INTEGER,
    ADD COLUMN qualifiers_per_pool INTEGER,
    ADD COLUMN games_to_win        INTEGER     NOT NULL DEFAULT 2,
    ADD COLUMN points_to_win       INTEGER     NOT NULL DEFAULT 21,
    ADD COLUMN point_cap           INTEGER     NOT NULL DEFAULT 30;

ALTER TABLE event
    ALTER COLUMN format DROP DEFAULT,
    ALTER COLUMN games_to_win DROP DEFAULT,
    ALTER COLUMN points_to_win DROP DEFAULT,
    ALTER COLUMN point_cap DROP DEFAULT;

ALTER TABLE event
    ADD CONSTRAINT event_format_check
        CHECK (format IN ('SINGLE_ELIMINATION', 'POOLS_THEN_ELIMINATION', 'ROUND_ROBIN', 'TEAM_TIE')),
    ADD CONSTRAINT event_pool_settings_check
        CHECK (
            (format = 'POOLS_THEN_ELIMINATION'
                AND pool_size BETWEEN 3 AND 8
                AND qualifiers_per_pool BETWEEN 1 AND pool_size - 1)
            OR (format <> 'POOLS_THEN_ELIMINATION'
                AND pool_size IS NULL
                AND qualifiers_per_pool IS NULL)
        ),
    ADD CONSTRAINT event_scoring_check
        CHECK (games_to_win BETWEEN 1 AND 3
            AND points_to_win BETWEEN 1 AND 50
            AND point_cap BETWEEN points_to_win AND 50);
