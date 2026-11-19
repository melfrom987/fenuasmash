package com.tao.fenuasmash.tablet;

import static com.tao.fenuasmash.TestTokens.admin;
import static com.tao.fenuasmash.TestTokens.club;
import static com.tao.fenuasmash.TestTokens.organizer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.tao.fenuasmash.DatabaseCleaner;
import com.tao.fenuasmash.TestcontainersConfiguration;
import com.tao.fenuasmash.common.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TabletControllerTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PairingAttemptLimiter attemptLimiter;

	private UUID organizationId;

	private UUID tournamentId;

	private UUID courtId;

	@BeforeEach
	void setUp() {
		DatabaseCleaner.clean(jdbc);
		attemptLimiter.reset();
		organizationId = UUID.randomUUID();
		jdbc.update("INSERT INTO organization (id, name, created_at) VALUES (?, ?, ?)", organizationId,
				"Ligue de Badminton de Polynésie", Timestamp.from(Instant.now()));
		tournamentId = insertTournament("Open de Papeete");
		courtId = insertCourt(tournamentId, 1, true);
	}

	// ------------------------------------------------------------ Pairing codes

	@Test
	void createPairingCodeReturnsShortCodeValidForTenMinutes() throws Exception {
		Instant before = Instant.now();

		String body = mvc.perform(post("/api/courts/{id}/pairing-code", courtId).with(organizer(jwtService)))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
			.andExpect(jsonPath("$.code").value(matchesPattern("^[A-HJKMNP-Z2-9]{6}$")))
			.andExpect(jsonPath("$.courtId").value(courtId.toString()))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Instant expiresAt = Instant.parse(JsonPath.read(body, "$.expiresAt"));
		assertThat(expiresAt).isBetween(before.plus(TabletService.CODE_TTL).minusSeconds(1),
				Instant.now().plus(TabletService.CODE_TTL));
	}

	@Test
	void pairingCodeIsStoredHashed() throws Exception {
		String code = createCode(courtId);

		String stored = jdbc.queryForObject("SELECT code_hash FROM court_pairing_code WHERE court_id = ?",
				String.class, courtId);
		assertThat(stored).isNotEqualTo(code).isEqualTo(TabletSecrets.hash(code)).hasSize(64);
	}

	@Test
	void newPairingCodeReplacesThePreviousOne() throws Exception {
		String first = createCode(courtId);
		String second = createCode(courtId);

		pair(first).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PAIRING_CODE"));
		pair(second).andExpect(status().isOk());
		assertThat(countRows("court_pairing_code")).isZero();
	}

	@Test
	void createPairingCodeReturnsNotFoundForUnknownCourt() throws Exception {
		mvc.perform(post("/api/courts/{id}/pairing-code", UUID.randomUUID()).with(organizer(jwtService)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURT_NOT_FOUND"));
	}

	@Test
	void createPairingCodeRejectsInactiveCourt() throws Exception {
		UUID inactive = insertCourt(tournamentId, 2, false);

		mvc.perform(post("/api/courts/{id}/pairing-code", inactive).with(organizer(jwtService)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURT_INACTIVE"));
	}

	@Test
	void onlyOrganizersCreatePairingCodes() throws Exception {
		mvc.perform(post("/api/courts/{id}/pairing-code", courtId))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		mvc.perform(post("/api/courts/{id}/pairing-code", courtId).with(club(jwtService)))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/courts/{id}/pairing-code", courtId).header(HttpHeaders.AUTHORIZATION,
				"Bearer " + pairNewTablet(courtId)))
			.andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------ Pairing

	@Test
	void pairReturnsTokenAndCourt() throws Exception {
		String code = createCode(courtId);

		pair(code, "Galaxy Tab A8").andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
			.andExpect(jsonPath("$.tabletId").isNotEmpty())
			.andExpect(jsonPath("$.accessToken").value(startsWith("fst_")))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.court.id").value(courtId.toString()))
			.andExpect(jsonPath("$.court.tournamentId").value(tournamentId.toString()))
			.andExpect(jsonPath("$.court.number").value(1));

		assertThat(jdbc.queryForObject("SELECT device_name FROM tablet", String.class)).isEqualTo("Galaxy Tab A8");
	}

	@Test
	void tokenIsStoredHashed() throws Exception {
		String token = pairNewTablet(courtId);

		Map<String, Object> row = jdbc.queryForMap("SELECT * FROM tablet");
		assertThat(row.values()).doesNotContain(token);
		assertThat(row.get("token_hash")).isEqualTo(TabletSecrets.hash(token));
	}

	@Test
	void pairAcceptsCodeInLowerCaseWithSpacesAndDashes() throws Exception {
		String code = createCode(courtId);
		String typed = " " + code.substring(0, 3).toLowerCase() + "-" + code.substring(3).toLowerCase() + " ";

		pair(typed).andExpect(status().isOk());
	}

	@Test
	void codePairsOneTabletOnly() throws Exception {
		String code = createCode(courtId);

		pair(code).andExpect(status().isOk());
		pair(code).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PAIRING_CODE"));
		assertThat(countRows("tablet")).isEqualTo(1);
	}

	@Test
	void expiredCodeIsRejected() throws Exception {
		String code = createCode(courtId);
		jdbc.update("UPDATE court_pairing_code SET expires_at = ?", Timestamp.from(Instant.now().minusSeconds(1)));

		pair(code).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PAIRING_CODE"));
	}

	@Test
	void wrongCodeIsRejected() throws Exception {
		createCode(courtId);

		pair("AAAAAA").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_PAIRING_CODE"));
	}

	@Test
	void pairValidatesTheRequest() throws Exception {
		mvc.perform(post("/api/tablets/pair").contentType(MediaType.APPLICATION_JSON).content("{\"code\": \"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void tooManyWrongCodesBlockPairingEvenWithTheRightCode() throws Exception {
		String code = createCode(courtId);
		for (int i = 0; i < PairingAttemptLimiter.MAX_FAILURES_PER_IP; i++) {
			pair("AAAAAA").andExpect(status().isBadRequest());
		}

		pair(code).andExpect(status().isTooManyRequests())
			.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
			.andExpect(jsonPath("$.code").value("TOO_MANY_PAIRING_ATTEMPTS"));
	}

	@Test
	void pairRejectsCourtDeactivatedAfterTheCodeWasCreated() throws Exception {
		String code = createCode(courtId);
		jdbc.update("UPDATE court SET active = FALSE WHERE id = ?", courtId);

		pair(code).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COURT_INACTIVE"));
		assertThat(countRows("tablet")).isZero();
	}

	@Test
	void pairingNewTabletRevokesThePreviousOneOfTheCourt() throws Exception {
		String previous = pairNewTablet(courtId);
		String otherCourt = pairNewTablet(insertCourt(tournamentId, 2, true));

		String current = pairNewTablet(courtId);

		me(previous).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("TABLET_REVOKED"));
		me(current).andExpect(status().isOk());
		me(otherCourt).andExpect(status().isOk());
	}

	// ------------------------------------------------------------ Tablet requests

	@Test
	void meReturnsTheTabletAndItsCourt() throws Exception {
		String body = pair(createCode(courtId), "Galaxy Tab A8").andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(body, "$.accessToken");

		me(token).andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value((String) JsonPath.read(body, "$.tabletId")))
			.andExpect(jsonPath("$.deviceName").value("Galaxy Tab A8"))
			.andExpect(jsonPath("$.pairedAt").isNotEmpty())
			.andExpect(jsonPath("$.court.id").value(courtId.toString()))
			.andExpect(jsonPath("$.court.number").value(1));
	}

	@Test
	void everyRequestUpdatesLastActivity() throws Exception {
		String token = pairNewTablet(courtId);
		Instant past = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);
		jdbc.update("UPDATE tablet SET last_seen_at = ?", Timestamp.from(past));

		mvc.perform(get("/api/tournaments/{id}/courts", tournamentId).header(HttpHeaders.AUTHORIZATION,
				"Bearer " + token))
			.andExpect(status().isOk());

		Instant lastSeen = jdbc.queryForObject("SELECT last_seen_at FROM tablet", Timestamp.class).toInstant();
		assertThat(lastSeen).isAfter(past.plus(Duration.ofMinutes(59)));
	}

	@Test
	void unknownTabletTokenIsUnauthorized() throws Exception {
		me("fst_unknown").andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void tabletCannotManageTournamentsNorReadAccounts() throws Exception {
		String token = pairNewTablet(courtId);

		mvc.perform(post("/api/tournaments/{id}/courts", tournamentId).header(HttpHeaders.AUTHORIZATION,
				"Bearer " + token))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/tournaments/{id}/tablets", tournamentId).header(HttpHeaders.AUTHORIZATION,
				"Bearer " + token))
			.andExpect(status().isForbidden());
	}

	@Test
	void meIsForTabletsOnly() throws Exception {
		mvc.perform(get("/api/tablets/me")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/tablets/me").with(organizer(jwtService))).andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------ Revocation

	@Test
	void revokedTabletIsRejectedOnEveryRequest() throws Exception {
		String body = pair(createCode(courtId)).andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(body, "$.accessToken");
		String tabletId = JsonPath.read(body, "$.tabletId");

		mvc.perform(post("/api/tablets/{id}/revoke", tabletId).with(organizer(jwtService)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(tabletId))
			.andExpect(jsonPath("$.revokedAt").isNotEmpty());

		me(token).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("TABLET_REVOKED"));
		// Even public reads: the tablet must know at once that it has to be paired again
		mvc.perform(get("/api/tournaments").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("TABLET_REVOKED"));
	}

	@Test
	void revokingTwiceKeepsTheFirstDate() throws Exception {
		pairNewTablet(courtId);
		UUID tabletId = jdbc.queryForObject("SELECT id FROM tablet", UUID.class);
		mvc.perform(post("/api/tablets/{id}/revoke", tabletId).with(organizer(jwtService)));
		Timestamp first = jdbc.queryForObject("SELECT revoked_at FROM tablet", Timestamp.class);

		mvc.perform(post("/api/tablets/{id}/revoke", tabletId).with(organizer(jwtService)))
			.andExpect(status().isOk());

		assertThat(jdbc.queryForObject("SELECT revoked_at FROM tablet", Timestamp.class)).isEqualTo(first);
	}

	@Test
	void revokeReturnsNotFoundForUnknownTablet() throws Exception {
		mvc.perform(post("/api/tablets/{id}/revoke", UUID.randomUUID()).with(organizer(jwtService)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TABLET_NOT_FOUND"));
	}

	@Test
	void onlyOrganizersRevokeTablets() throws Exception {
		pairNewTablet(courtId);
		UUID tabletId = jdbc.queryForObject("SELECT id FROM tablet", UUID.class);

		mvc.perform(post("/api/tablets/{id}/revoke", tabletId)).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/tablets/{id}/revoke", tabletId).with(admin(jwtService)))
			.andExpect(status().isForbidden());
		assertThat(countRevoked()).isZero();
	}

	// ------------------------------------------------------------ List

	@Test
	void listReturnsTabletsByCourtThenLatestPairingFirst() throws Exception {
		UUID secondCourt = insertCourt(tournamentId, 2, true);
		pairNewTablet(secondCourt);
		pairNewTablet(courtId);
		pairNewTablet(courtId);
		pairNewTablet(insertCourt(insertTournament("Open de Moorea"), 1, true));

		String body = mvc.perform(get("/api/tournaments/{id}/tablets", tournamentId).with(organizer(jwtService)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[0].courtId").value(courtId.toString()))
			.andExpect(jsonPath("$[0].courtNumber").value(1))
			.andExpect(jsonPath("$[0].revokedAt").doesNotExist())
			.andExpect(jsonPath("$[0].lastSeenAt").isNotEmpty())
			.andExpect(jsonPath("$[1].courtNumber").value(1))
			.andExpect(jsonPath("$[1].revokedAt").isNotEmpty())
			.andExpect(jsonPath("$[2].courtId").value(secondCourt.toString()))
			.andExpect(jsonPath("$[2].courtNumber").value(2))
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(body).doesNotContain("token");
	}

	@Test
	void listReturnsEmptyArrayWithoutTablets() throws Exception {
		mvc.perform(get("/api/tournaments/{id}/tablets", tournamentId).with(organizer(jwtService)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void listReturnsNotFoundForUnknownTournament() throws Exception {
		mvc.perform(get("/api/tournaments/{id}/tablets", UUID.randomUUID()).with(organizer(jwtService)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TOURNAMENT_NOT_FOUND"));
	}

	@Test
	void listIsForOrganizersOnly() throws Exception {
		mvc.perform(get("/api/tournaments/{id}/tablets", tournamentId)).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/tournaments/{id}/tablets", tournamentId).with(club(jwtService)))
			.andExpect(status().isForbidden());
	}

	@Test
	void deletingTheCourtRemovesItsTabletsAndCode() throws Exception {
		String token = pairNewTablet(courtId);
		createCode(courtId);

		jdbc.update("DELETE FROM court WHERE id = ?", courtId);

		assertThat(countRows("tablet")).isZero();
		assertThat(countRows("court_pairing_code")).isZero();
		me(token).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	// ------------------------------------------------------------ Helpers

	private String createCode(UUID court) throws Exception {
		String body = mvc.perform(post("/api/courts/{id}/pairing-code", court).with(organizer(jwtService)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.code");
	}

	private ResultActions pair(String code) throws Exception {
		return pair(code, null);
	}

	private ResultActions pair(String code, String deviceName) throws Exception {
		String deviceJson = deviceName == null ? "null" : "\"" + deviceName + "\"";
		return mvc.perform(post("/api/tablets/pair").contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\": \"" + code + "\", \"deviceName\": " + deviceJson + "}"));
	}

	/** Pairs a tablet with the court and returns its token. */
	private String pairNewTablet(UUID court) throws Exception {
		String body = pair(createCode(court)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	private ResultActions me(String token) throws Exception {
		MockHttpServletRequestBuilder request = get("/api/tablets/me").header(HttpHeaders.AUTHORIZATION,
				"Bearer " + token);
		return mvc.perform(request);
	}

	private int countRows(String table) {
		List<Integer> counts = jdbc.queryForList("SELECT COUNT(*)::int FROM " + table, Integer.class);
		return counts.getFirst();
	}

	private int countRevoked() {
		return jdbc.queryForObject("SELECT COUNT(*)::int FROM tablet WHERE revoked_at IS NOT NULL", Integer.class);
	}

	private UUID insertTournament(String name) {
		UUID id = UUID.randomUUID();
		Instant startsAt = Instant.now().plus(30, ChronoUnit.DAYS);
		Timestamp now = Timestamp.from(Instant.now());
		jdbc.update(
				"""
						INSERT INTO tournament (id, organization_id, name, starts_at, ends_at, minimum_rest_minutes,
						    competition_type, draw_format, play_mode, games_to_win, points_to_win, two_point_lead, point_cap,
						    created_at, updated_at)
						VALUES (?, ?, ?, ?, ?, ?, 'INDIVIDUAL', 'SINGLE_ELIMINATION', 'SETS', 2, 21, TRUE, 30, ?, ?)
						""",
				id, organizationId, name, Timestamp.from(startsAt), Timestamp.from(startsAt.plus(2, ChronoUnit.DAYS)), 20,
				now, now);
		return id;
	}

	private UUID insertCourt(UUID tournament, int number, boolean active) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO court (id, tournament_id, number, active) VALUES (?, ?, ?, ?)", id, tournament, number,
				active);
		return id;
	}

}
