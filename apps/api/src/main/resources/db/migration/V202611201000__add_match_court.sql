-- The court of a match. A match is on its court while CALLED, WARMUP or
-- IN_PROGRESS; it keeps the court once over, as the place it was played.
-- A court that received a match cannot be deleted (disable it instead).

ALTER TABLE match ADD COLUMN court_id UUID REFERENCES court (id);

ALTER TABLE match ADD CONSTRAINT match_court_check CHECK (
    (status NOT IN ('CALLED', 'WARMUP', 'IN_PROGRESS') OR court_id IS NOT NULL)
    AND (status NOT IN ('SCHEDULED', 'READY') OR court_id IS NULL));

-- One match at most on each court
CREATE UNIQUE INDEX match_court_occupied_idx ON match (court_id)
    WHERE status IN ('CALLED', 'WARMUP', 'IN_PROGRESS');
