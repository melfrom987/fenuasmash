package com.tao.fenuasmash.court;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourtService {

	private final CourtRepository repository;

	CourtService(CourtRepository repository) {
		this.repository = repository;
	}

}
