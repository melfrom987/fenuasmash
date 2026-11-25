package com.tao.fenuasmash.draw;

import java.util.List;
import java.util.UUID;

/** The `PoolStanding` schema. */
record PoolStandingResponse(String pool, int qualifiers, boolean complete, List<Row> rows) {

	record Row(int rank, UUID registrationId, int played, int won, int lost, int gamesWon, int gamesLost,
			int pointsWon, int pointsLost, boolean decidedByLot) {

		static Row from(PoolStandings.Row row) {
			return new Row(row.rank(), row.registrationId(), row.played(), row.won(), row.lost(), row.gamesWon(),
					row.gamesLost(), row.pointsWon(), row.pointsLost(), row.decidedByLot());
		}

	}

}
