package com.tao.fenuasmash.registration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.tao.fenuasmash.TestTokens.club;
import static com.tao.fenuasmash.TestTokens.organizer;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import com.jayway.jsonpath.JsonPath;
import com.tao.fenuasmash.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrationChangeTests extends RegistrationTestSupport {

	@Test
	void setsAndRemovesThePartner() throws Exception {
		UUID registration = registered(createEvent("Double Mixte", "XD"), teva, null);

		update(registration, "{\"partnerId\": \"" + hina + "\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andExpect(jsonPath("$.partner.id").value(hina.toString()));
		update(registration, "{\"partnerId\": \"" + manu + "\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PARTNER_NOT_ELIGIBLE"));
		update(registration, "{\"partnerId\": null}").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("LOOKING_FOR_PARTNER"))
			.andExpect(jsonPath("$.partner").value(nullValue()));
	}

	@Test
	void refusesAPartnerAlreadyInTheEventOrThePlayerHimself() throws Exception {
		UUID open = createEvent("Double Open", "OD");
		registered(open, hina, null);
		UUID registration = registered(open, teva, null);

		update(registration, "{\"partnerId\": \"" + hina + "\"}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PARTNER_ALREADY_REGISTERED"));
		update(registration, "{\"partnerId\": \"" + teva + "\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void confirmsOnlyAPendingPair() throws Exception {
		UUID registration = registered(createEvent("Double Mixte", "XD"), teva, hina);

		update(registration, "{\"confirm\": true}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("NOT_PENDING"));

		jdbc.update("UPDATE registration SET status = 'PENDING_PARTNER' WHERE id = ?", registration);
		update(registration, "{\"confirm\": true}").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CONFIRMED"));
	}

	@Test
	void withdrawsARegistration() throws Exception {
		UUID event = createEvent("Simple Homme", "MS");
		UUID registration = registered(event, teva, null);

		mvc.perform(delete("/api/registrations/{id}", registration).with(organizer(jwtService)))
			.andExpect(status().isNoContent());
		mvc.perform(delete("/api/registrations/{id}", registration).with(organizer(jwtService)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("REGISTRATION_NOT_FOUND"));
		registered(event, teva, null);
	}

	@Test
	void movesRegistrationsAndRejectsThoseThatDoNotFit() throws Exception {
		UUID minime = createAgeEvent("Simple Minime", "MS", 2011, null);
		UUID cadet = createAgeEvent("Simple Cadet", "MS", 2009, null);
		UUID senior = createEvent("Simple Homme", "MS");
		UUID manuInMinime = registered(minime, manu, null);
		UUID tevaInSenior = registered(senior, teva, null);
		UUID pair = registered(createEvent("Double Mixte", "XD"), teva, hina);
		String registeredAt = JsonPath.read(mvc.perform(get("/api/events/{id}/registrations", minime)
			.with(organizer(jwtService))).andReturn().getResponse().getContentAsString(), "$[0].registeredAt");

		transfer(cadet, List.of(manuInMinime, tevaInSenior, pair, UUID.randomUUID())).andExpect(status().isOk())
			.andExpect(jsonPath("$.moved", hasSize(1)))
			.andExpect(jsonPath("$.moved[0].id").value(manuInMinime.toString()))
			.andExpect(jsonPath("$.moved[0].eventId").value(cadet.toString()))
			.andExpect(jsonPath("$.moved[0].status").value("CONFIRMED"))
			.andExpect(jsonPath("$.moved[0].registeredAt").value(registeredAt))
			.andExpect(jsonPath("$.rejected", hasSize(3)))
			.andExpect(jsonPath("$.rejected[0].registrationId").value(tevaInSenior.toString()))
			.andExpect(jsonPath("$.rejected[0].code").value("PLAYER_NOT_ELIGIBLE"))
			.andExpect(jsonPath("$.rejected[1].code").value("INCOMPATIBLE_EVENT"))
			.andExpect(jsonPath("$.rejected[2].code").value("REGISTRATION_NOT_FOUND"));

		// Playing up from Cadet to Senior; once there, another registration of the player is refused
		transfer(senior, List.of(manuInMinime)).andExpect(jsonPath("$.moved", hasSize(1)));
		UUID again = registered(cadet, manu, null);
		transfer(senior, List.of(again)).andExpect(jsonPath("$.rejected[0].code").value("ALREADY_REGISTERED"));
	}

	@Test
	void movesNoMoreThanTheEventCanTake() throws Exception {
		UUID source = createEvent("Double Open", "OD");
		UUID target = createEvent("Double Open Série 1", "OD", 2);
		UUID first = registered(source, teva, null);
		UUID second = registered(source, hina, null);
		UUID third = registered(source, moana, null);

		transfer(target, List.of(first, second, third)).andExpect(jsonPath("$.moved", hasSize(2)))
			.andExpect(jsonPath("$.rejected[0].registrationId").value(third.toString()))
			.andExpect(jsonPath("$.rejected[0].code").value("EVENT_FULL"));
	}

	@Test
	void listsTheEligiblePlayersAndPartners() throws Exception {
		UUID mixed = createAgeEvent("Double Mixte Senior", "XD", 1990, null);
		registered(mixed, teva, null);
		String url = "/api/events/{id}/eligible-players";

		// Born in 1990 or later and not registered yet: Manu and Hina, not Moana (1985)
		mvc.perform(get(url, mixed).with(organizer(jwtService)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalItems").value(2))
			.andExpect(jsonPath("$.items[0].firstName").value("Manu"))
			.andExpect(jsonPath("$.items[1].firstName").value("Hina"));
		// A partner for Teva in mixed doubles: a woman
		mvc.perform(get(url, mixed).param("partnerOf", teva.toString()).with(club(jwtService)))
			.andExpect(jsonPath("$.totalItems").value(1))
			.andExpect(jsonPath("$.items[0].firstName").value("Hina"));
		mvc.perform(get(url, mixed).param("q", "ah sing").with(organizer(jwtService)))
			.andExpect(jsonPath("$.totalItems").value(1));
	}

	@Test
	void listsOnlyTheRightGenderInSinglesAndNoPartner() throws Exception {
		UUID women = createEvent("Simple Dame", "WS");
		String url = "/api/events/{id}/eligible-players";

		mvc.perform(get(url, women).with(organizer(jwtService))).andExpect(jsonPath("$.totalItems").value(2));
		mvc.perform(get(url, women).param("partnerOf", hina.toString()).with(organizer(jwtService)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

}
