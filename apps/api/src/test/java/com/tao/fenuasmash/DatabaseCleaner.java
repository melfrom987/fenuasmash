package com.tao.fenuasmash;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

public final class DatabaseCleaner {

	private DatabaseCleaner() {
	}

	public static void clean(JdbcTemplate jdbc) {
		List<String> tables = jdbc.queryForList(
				"SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'",
				String.class);
		if (!tables.isEmpty()) {
			jdbc.execute("TRUNCATE TABLE " + String.join(", ", tables) + " CASCADE");
		}
	}

}
