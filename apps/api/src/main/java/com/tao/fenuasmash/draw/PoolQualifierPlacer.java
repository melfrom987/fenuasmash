package com.tao.fenuasmash.draw;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.tao.fenuasmash.match.PoolMatchChanged;

/** Places the qualifiers of a pool in the bracket as soon as its last match is over. */
@Component
class PoolQualifierPlacer {

	private final DrawService drawService;

	PoolQualifierPlacer(DrawService drawService) {
		this.drawService = drawService;
	}

	@EventListener
	void onPoolMatchChanged(PoolMatchChanged change) {
		drawService.placeQualifiers(change.eventId(), change.pool());
	}

}
