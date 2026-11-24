-- Umpire tablets, paired with a court through a one-time code. Codes and tokens
-- are stored as SHA-256 hashes only: a database leak gives no usable secret.

-- At most one code per court: creating a new code replaces the previous one.
CREATE TABLE court_pairing_code (
    court_id   UUID        PRIMARY KEY REFERENCES court (id) ON DELETE CASCADE,
    code_hash  VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT court_pairing_code_code_hash_key UNIQUE (code_hash)
);

CREATE TABLE tablet (
    id           UUID         PRIMARY KEY,
    court_id     UUID         NOT NULL REFERENCES court (id) ON DELETE CASCADE,
    device_name  VARCHAR(100),
    token_hash   VARCHAR(64)  NOT NULL,
    paired_at    TIMESTAMPTZ  NOT NULL,
    last_seen_at TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ,
    CONSTRAINT tablet_token_hash_key UNIQUE (token_hash)
);

CREATE INDEX tablet_court_id_idx ON tablet (court_id);

-- One paired tablet per court at most
CREATE UNIQUE INDEX tablet_one_active_per_court_idx ON tablet (court_id) WHERE revoked_at IS NULL;
