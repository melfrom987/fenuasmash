package com.tao.fenuasmash.match;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ScoreEventRepository extends JpaRepository<ScoreEvent, UUID> {

	List<ScoreEvent> findAllByMatchIdOrderBySequence(UUID matchId);

}
