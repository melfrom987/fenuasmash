package com.tao.fenuasmash.court;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CourtRepository extends JpaRepository<Court, UUID> {

	List<Court> findAllByTournamentIdOrderByNumber(UUID tournamentId);

}
