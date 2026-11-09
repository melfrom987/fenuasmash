package com.tao.fenuasmash.tournament;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TournamentRepository extends JpaRepository<Tournament, UUID> {
}
