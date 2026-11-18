package com.tao.fenuasmash.account;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tao.fenuasmash.common.error.TooManyRequestsException;

/**
 * Slows down password guessing by counting failed logins over a 15-minute
 * window: 5 per email, to protect one account, and 20 per client IP, to stop
 * one client from trying many accounts. Once a limit is reached, every login
 * for that email or IP is refused until the window ends, even with the right
 * password.
 * <p>
 * Counters live in memory: they reset when the API restarts, which is enough
 * for a single instance.
 */
@Component
class LoginAttemptLimiter {

	static final Duration WINDOW = Duration.ofMinutes(15);

	static final int MAX_FAILURES_PER_EMAIL = 5;

	static final int MAX_FAILURES_PER_IP = 20;

	/** Above this size, expired counters are dropped so that memory stays bounded. */
	private static final int CLEANUP_THRESHOLD = 10_000;

	private final Map<String, Failures> failures = new ConcurrentHashMap<>();

	private final Clock clock;

	@Autowired
	LoginAttemptLimiter() {
		this(Clock.systemUTC());
	}

	LoginAttemptLimiter(Clock clock) {
		this.clock = clock;
	}

	/**
	 * @throws TooManyRequestsException if the email or the IP has too many recent
	 * failures
	 */
	void checkAllowed(String email, String clientIp) {
		Instant now = clock.instant();
		check(emailKey(email), MAX_FAILURES_PER_EMAIL, now);
		check(ipKey(clientIp), MAX_FAILURES_PER_IP, now);
	}

	void recordFailure(String email, String clientIp) {
		Instant now = clock.instant();
		if (failures.size() > CLEANUP_THRESHOLD) {
			failures.values().removeIf(entry -> entry.isExpired(now));
		}
		increment(emailKey(email), now);
		increment(ipKey(clientIp), now);
	}

	/**
	 * A successful login clears the email counter, so that a few typos do not add
	 * up over the day. The IP counter is kept: it would otherwise reset on any
	 * account the client controls.
	 */
	void recordSuccess(String email) {
		failures.remove(emailKey(email));
	}

	/** For tests: forgets every counter. */
	void reset() {
		failures.clear();
	}

	private void check(String key, int limit, Instant now) {
		Failures entry = failures.get(key);
		if (entry != null && !entry.isExpired(now) && entry.count() >= limit) {
			throw new TooManyRequestsException("TOO_MANY_LOGIN_ATTEMPTS", "Too many failed logins, try again later",
					Duration.between(now, entry.windowStart().plus(WINDOW)));
		}
	}

	private void increment(String key, Instant now) {
		failures.compute(key, (k, entry) -> entry == null || entry.isExpired(now) ? new Failures(now, 1)
				: new Failures(entry.windowStart(), entry.count() + 1));
	}

	private static String emailKey(String email) {
		return "email:" + email;
	}

	private static String ipKey(String clientIp) {
		return "ip:" + clientIp;
	}

	private record Failures(Instant windowStart, int count) {

		boolean isExpired(Instant now) {
			return !now.isBefore(windowStart.plus(WINDOW));
		}

	}

}
