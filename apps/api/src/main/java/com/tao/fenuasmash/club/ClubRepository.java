package com.tao.fenuasmash.club;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ClubRepository extends JpaRepository<Club, UUID> {

	List<Club> findAllByOrderByNameKeyAsc();

	List<Club> findAllByNameKeyContainingOrderByNameKeyAsc(String nameKey);

	boolean existsByNameKey(String nameKey);

	boolean existsByNameKeyAndIdNot(String nameKey, UUID id);

}
