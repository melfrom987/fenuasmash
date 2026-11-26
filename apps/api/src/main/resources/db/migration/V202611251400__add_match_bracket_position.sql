-- Position of a knockout match in its round (#92), from 1 at the top of the bracket: the
-- winners of positions 2k-1 and 2k meet at position k of the next round.
ALTER TABLE match
    ADD COLUMN bracket_position INTEGER,
    ADD CONSTRAINT match_bracket_position_check
        CHECK (bracket_position IS NULL OR (bracket_position > 0 AND round IS NOT NULL));

CREATE UNIQUE INDEX match_event_id_round_bracket_position_key ON match (event_id, round, bracket_position)
    WHERE bracket_position IS NOT NULL;
