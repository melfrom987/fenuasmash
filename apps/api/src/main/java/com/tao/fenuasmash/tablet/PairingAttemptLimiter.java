package com.tao.fenuasmash.tablet;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tao.fenuasmash.common.error.TooManyRequestsException;

/**
 * Stops code guessing: after 10 wrong codes from one client IP within 15
 * minutes, every pairing from that IP is refused until the window ends, even
 * with a right code. A successful pairing does not reset the counter.
 * <p>
 * Counters live in memory: they reset when the API restarts, which is enough
 * for a single instance.
 */
@Component
class PairingAttemptLimiter {

	static final Duration WINDOW = Duration.ofMinutes(15);

	static final int MAX_FAILURES_PER_IP = 10;

	/** Above this size, expired counters are dropped so that memory stays bounded. */
	private static final int CLEANUP_THRESHOLD = 10_000;

	private final Map<String, Failures> failures = new ConcurrentHashMap<>();

	private final Clock clock;

	@Autowired
	PairingAttemptLimiter() {
		this(Clock.systemUTC());
	}

	PairingAttemptLimiter(Clock clock) {
		this.clock = clock;
	}

	/**
	 * @throws TooManyRequestsException if the IP has too many recent failures
	 */
	void checkAllowed(String clientIp) {
		Instant now = clock.instant();
		Failures entry = failures.get(clientIp);
		if (entry != null && !entry.isExpired(now) && entry.count() >= MAX_FAILURES_PER_IP) {
			throw new TooManyRequestsException("TOO_MANY_PAIRING_ATTEMPTS",
					"Too many wrong pairing codes, try again later",
					Duration.between(now, entry.windowStart().plus(WINDOW)));
		}
	}

	void recordFailure(String clientIp) {
		Instant now = clock.instant();
		if (failures.size() > CLEANUP_THRESHOLD) {
			failures.values().removeIf(entry -> entry.isExpired(now));
		}
		failures.compute(clientIp, (ip, entry) -> entry == null || entry.isExpired(now) ? new Failures(now, 1)
				: new Failures(entry.windowStart(), entry.count() + 1));
	}

	/** For tests: forgets every counter. */
	void reset() {
		failures.clear();
	}

	private record Failures(Instant windowStart, int count) {

		boolean isExpired(Instant now) {
			return !now.isBefore(windowStart.plus(WINDOW));
		}

	}

}
