CREATE TABLE organization (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);
