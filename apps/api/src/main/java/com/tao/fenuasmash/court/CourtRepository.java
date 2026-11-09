package com.tao.fenuasmash.court;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CourtRepository extends JpaRepository<Court, UUID> {
}
