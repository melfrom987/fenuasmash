package com.tao.fenuasmash.event;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EventRepository extends JpaRepository<Event, UUID> {

	List<Event> findByTournamentId(UUID tournamentId);

	boolean existsByTournamentIdAndNameIgnoreCase(UUID tournamentId, String name);

	boolean existsByTournamentIdAndNameIgnoreCaseAndIdNot(UUID tournamentId, String name, UUID id);

}
