CREATE TABLE tournament_forbidden_combination (
    tournament_id     UUID       NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    sort_order        INTEGER    NOT NULL,
    first_discipline  VARCHAR(2) NOT NULL,
    second_discipline VARCHAR(2) NOT NULL,
    PRIMARY KEY (tournament_id, sort_order),
    CONSTRAINT tournament_forbidden_combination_first_check
        CHECK (first_discipline IN ('MS', 'WS', 'MD', 'WD', 'XD', 'OD')),
    CONSTRAINT tournament_forbidden_combination_second_check
        CHECK (second_discipline IN ('MS', 'WS', 'MD', 'WD', 'XD', 'OD'))
);
