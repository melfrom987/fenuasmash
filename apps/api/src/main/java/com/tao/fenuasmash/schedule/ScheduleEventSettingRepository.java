package com.tao.fenuasmash.schedule;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface ScheduleEventSettingRepository extends JpaRepository<ScheduleEventSetting, UUID> {

	List<ScheduleEventSetting> findAllByTournamentIdOrderByPosition(UUID tournamentId);

	/** The stage starts go with them ({@code ON DELETE CASCADE}). */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM ScheduleEventSetting s WHERE s.tournamentId = :tournamentId")
	void deleteAllOfTournament(UUID tournamentId);

}
