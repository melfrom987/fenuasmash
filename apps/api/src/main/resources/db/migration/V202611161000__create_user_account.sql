-- Login accounts. Organizers belong to a federation (organization); links to
-- clubs and players will be added once those tables exist.

CREATE TABLE user_account (
    id              UUID         PRIMARY KEY,
    email           VARCHAR(254) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    organization_id UUID         REFERENCES organization (id),
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT user_account_role_check
        CHECK (role IN ('ADMIN', 'ORGANIZER', 'CLUB', 'PLAYER')),
    CONSTRAINT user_account_email_lowercase_check
        CHECK (email = lower(email)),
    CONSTRAINT user_account_organization_check
        CHECK ((role = 'ORGANIZER') = (organization_id IS NOT NULL))
);

CREATE UNIQUE INDEX user_account_email_key ON user_account (email);

CREATE INDEX user_account_organization_id_idx ON user_account (organization_id);
