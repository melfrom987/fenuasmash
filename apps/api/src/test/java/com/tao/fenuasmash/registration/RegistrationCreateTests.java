package com.tao.fenuasmash.registration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.tao.fenuasmash.TestTokens.club;
import static com.tao.fenuasmash.TestTokens.organizer;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import com.tao.fenuasmash.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrationCreateTests extends RegistrationTestSupport {

	@Test
	void registersASinglesPlayerConfirmed() throws Exception {
		UUID event = createEvent("Simple Homme", "MS");

		register(event, teva, null).andExpect(status().isCreated())
			.andExpect(jsonPath("$.tournamentId").value(tournamentId.toString()))
			.andExpect(jsonPath("$.eventId").value(event.toString()))
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andExpect(jsonPath("$.player.id").value(teva.toString()))
			.andExpect(jsonPath("$.player.clubName").value("AS Pirae"))
			.andExpect(jsonPath("$.player.rankings.singles.points").value(900))
			.andExpect(jsonPath("$.partner").value(nullValue()))
			.andExpect(jsonPath("$.registeredAt").isNotEmpty());
	}

	@Test
	void refusesAPartnerInSingles() throws Exception {
		register(createEvent("Simple Homme", "MS"), teva, manu).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void registersAPairConfirmedOrAPlayerLookingForAPartner() throws Exception {
		register(createEvent("Double Mixte", "XD"), teva, hina).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andExpect(jsonPath("$.partner.id").value(hina.toString()));
		register(createEvent("Double Homme", "MD"), teva, null).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("LOOKING_FOR_PARTNER"));
	}

	@Test
	void checksTheGenderAgainstTheDiscipline() throws Exception {
		register(createEvent("Simple Homme", "MS"), hina, null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PLAYER_NOT_ELIGIBLE"));
		register(createEvent("Double Mixte", "XD"), teva, manu).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PARTNER_NOT_ELIGIBLE"));
		register(createEvent("Double Open", "OD"), hina, moana).andExpect(status().isCreated());
	}

	@Test
	void checksTheBirthYearBounds() throws Exception {
		UUID minime = createAgeEvent("Simple Minime", "MS", 2011, null);
		UUID veteran = createAgeEvent("Double Mixte Vétéran", "XD", null, 1990);

		register(minime, manu, null).andExpect(status().isCreated());
		register(minime, teva, null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PLAYER_NOT_ELIGIBLE"));
		register(veteran, manu, moana).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PLAYER_NOT_ELIGIBLE"));
		register(veteran, teva, hina).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PLAYER_NOT_ELIGIBLE"));
	}

	@Test
	void registersAPlayerInSeveralEventsButOnceInEach() throws Exception {
		UUID singles = createEvent("Simple Homme", "MS");
		UUID mixed = createEvent("Double Mixte", "XD");
		registered(singles, teva, null);
		registered(mixed, teva, hina);

		register(singles, teva, null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_REGISTERED"));
		register(mixed, manu, hina).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PARTNER_ALREADY_REGISTERED"));
		register(mixed, hina, null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_REGISTERED"));
	}

	@Test
	void refusesAFullEvent() throws Exception {
		UUID event = createEvent("Double Open", "OD", 2);
		registered(event, teva, null);
		registered(event, hina, null);

		register(event, moana, null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EVENT_FULL"));
	}

	@Test
	void refusesForbiddenCombinationsForThePlayerAndThePartner() throws Exception {
		jdbc.update("""
				INSERT INTO tournament_forbidden_combination (tournament_id, sort_order, first_discipline,
				    second_discipline) VALUES (?, 0, 'MS', 'XD')""", tournamentId);
		UUID singles = createEvent("Simple Homme", "MS");
		UUID mixed = createEvent("Double Mixte", "XD");
		registered(singles, teva, null);

		register(mixed, teva, hina).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FORBIDDEN_COMBINATION"));
		register(mixed, hina, teva).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FORBIDDEN_COMBINATION"));
		registered(mixed, manu, hina);
		register(singles, manu, null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FORBIDDEN_COMBINATION"));
	}

	@Test
	void returnsNotFoundForAnUnknownEventOrPlayer() throws Exception {
		register(UUID.randomUUID(), teva, null).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
		register(createEvent("Simple Homme", "MS"), UUID.randomUUID(), null).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PLAYER_NOT_FOUND"));
	}

	@Test
	void listsTheRegistrationsOfAnEventByRegistrationDate() throws Exception {
		UUID event = createEvent("Double Open", "OD");
		registered(event, moana, null);
		registered(event, teva, hina);

		mvc.perform(get("/api/events/{id}/registrations", event).with(club(jwtService)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].player.id").value(moana.toString()))
			.andExpect(jsonPath("$[1].partner.id").value(hina.toString()));
	}

	@Test
	void listsTheRegistrationsOfATournamentByEventWithFilters() throws Exception {
		UUID mixed = createEvent("Double Mixte", "XD");
		UUID singles = createEvent("Simple Homme", "MS");
		registered(mixed, teva, hina);
		registered(singles, teva, null);
		UUID doubles = createEvent("Double Homme", "MD");
		registered(doubles, manu, null);
		String url = "/api/tournaments/{id}/registrations";

		mvc.perform(get(url, tournamentId).with(organizer(jwtService)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[0].eventId").value(singles.toString()))
			.andExpect(jsonPath("$[1].eventId").value(doubles.toString()))
			.andExpect(jsonPath("$[2].eventId").value(mixed.toString()));
		mvc.perform(get(url, tournamentId).param("status", "LOOKING_FOR_PARTNER").with(organizer(jwtService)))
			.andExpect(jsonPath("$", hasSize(1)));
		mvc.perform(get(url, tournamentId).param("eventId", mixed.toString()).with(organizer(jwtService)))
			.andExpect(jsonPath("$", hasSize(1)));
		mvc.perform(get(url, tournamentId).param("playerId", hina.toString()).with(organizer(jwtService)))
			.andExpect(jsonPath("$", hasSize(1)));
		mvc.perform(get(url, UUID.randomUUID()).with(organizer(jwtService)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TOURNAMENT_NOT_FOUND"));
	}

}
