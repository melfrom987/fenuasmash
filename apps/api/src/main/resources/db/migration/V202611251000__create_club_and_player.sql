-- Clubs and players (#88). Names are unique and searched without case or accents: the
-- application stores a key (lower case, accents removed) next to each searchable text.

CREATE TABLE club (
    id       UUID PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    name_key VARCHAR(100) NOT NULL,
    city     VARCHAR(100) NOT NULL,
    address  VARCHAR(200),
    email    VARCHAR(254),
    phone    VARCHAR(30),
    CONSTRAINT club_name_key_key UNIQUE (name_key)
);

CREATE TABLE player (
    id               UUID PRIMARY KEY,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    gender           VARCHAR(10)  NOT NULL,
    birth_date       DATE         NOT NULL,
    club_id          UUID         NOT NULL REFERENCES club (id),
    license_number   VARCHAR(30),
    email            VARCHAR(254),
    phone            VARCHAR(30),
    singles_points   INTEGER,
    singles_position INTEGER,
    doubles_points   INTEGER,
    doubles_position INTEGER,
    mixed_points     INTEGER,
    mixed_position   INTEGER,
    search_key       VARCHAR(400) NOT NULL,
    CONSTRAINT player_gender_check CHECK (gender IN ('MALE', 'FEMALE')),
    CONSTRAINT player_license_number_key UNIQUE (license_number),
    CONSTRAINT player_points_check CHECK (singles_points >= 0 AND doubles_points >= 0 AND mixed_points >= 0),
    CONSTRAINT player_position_check
        CHECK (singles_position >= 1 AND doubles_position >= 1 AND mixed_position >= 1)
);

CREATE INDEX player_club_id_idx ON player (club_id);
CREATE INDEX player_last_name_first_name_idx ON player (lower(last_name), lower(first_name));
