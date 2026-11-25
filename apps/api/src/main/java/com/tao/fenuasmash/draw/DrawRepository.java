package com.tao.fenuasmash.draw;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface DrawRepository extends JpaRepository<Draw, UUID> {

}
