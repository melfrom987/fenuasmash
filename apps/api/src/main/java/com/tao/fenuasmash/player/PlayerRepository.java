package com.tao.fenuasmash.player;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface PlayerRepository extends JpaRepository<Player, UUID>, JpaSpecificationExecutor<Player> {

	boolean existsByLicenseNumber(String licenseNumber);

	boolean existsByLicenseNumberAndIdNot(String licenseNumber, UUID id);

}
