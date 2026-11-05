CREATE TABLE tournament (
    id                   UUID         PRIMARY KEY,
    organization_id      UUID         NOT NULL REFERENCES organization (id),
    name                 VARCHAR(200) NOT NULL,
    starts_at            TIMESTAMPTZ  NOT NULL,
    ends_at              TIMESTAMPTZ  NOT NULL,
    minimum_rest_minutes INTEGER      NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT tournament_dates_check CHECK (ends_at > starts_at),
    CONSTRAINT tournament_minimum_rest_check CHECK (minimum_rest_minutes >= 0)
);

CREATE INDEX tournament_organization_id_idx ON tournament (organization_id);

CREATE TABLE court (
    id            UUID    PRIMARY KEY,
    tournament_id UUID    NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    number        INTEGER NOT NULL,
    active        BOOLEAN NOT NULL,
    CONSTRAINT court_tournament_number_key UNIQUE (tournament_id, number),
    CONSTRAINT court_number_check CHECK (number > 0)
);
