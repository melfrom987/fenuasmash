package com.tao.fenuasmash.draw;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tao.fenuasmash.common.error.ConflictException;

/**
 * Called before the registrations of an event change: refused once its draw is published, and
 * the draft draw, if any, is deleted as it no longer matches the entries.
 * <p>
 * Not transactional on its own: it runs in the transaction of the change, and its refusal
 * does not mark that transaction for rollback (a transfer goes on with the other registrations).
 */
@Component
public class DrawLock {

	private final DrawRepository repository;

	DrawLock(DrawRepository repository) {
		this.repository = repository;
	}

	/** @throws ConflictException `DRAW_PUBLISHED` when the draw of the event is published */
	public void beforeRegistrationsChange(UUID eventId) {
		repository.findById(eventId).ifPresent((draw) -> {
			if (draw.getStatus() == DrawStatus.PUBLISHED) {
				throw published();
			}
			repository.delete(draw);
		});
	}

	static ConflictException published() {
		return new ConflictException("DRAW_PUBLISHED",
				"The draw of the event is published: its draw and its registrations are locked");
	}

}
