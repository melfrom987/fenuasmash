CREATE TABLE event (
    id            UUID         PRIMARY KEY,
    tournament_id UUID         NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    type          VARCHAR(2)   NOT NULL,
    CONSTRAINT event_type_check CHECK (type IN ('MS', 'WS', 'MD', 'WD', 'XD'))
);

CREATE UNIQUE INDEX event_tournament_id_name_key ON event (tournament_id, lower(name));
