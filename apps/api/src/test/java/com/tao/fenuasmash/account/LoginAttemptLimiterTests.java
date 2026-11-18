package com.tao.fenuasmash.account;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.tao.fenuasmash.common.error.TooManyRequestsException;

/**
 * Unit tests for {@link LoginAttemptLimiter}, with a clock the tests move
 * forward.
 */
class LoginAttemptLimiterTests {

	private static final String EMAIL = "orga@example.com";

	private static final String IP = "203.0.113.7";

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-14T08:00:00Z"));

	private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

	@Test
	void allowsLoginsBelowTheEmailLimit() {
		fail(EMAIL, IP, LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL - 1);

		assertAllowed(EMAIL, IP);
	}

	@Test
	void blocksTheEmailOnceItsLimitIsReached() {
		fail(EMAIL, IP, LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL);

		assertThatThrownBy(() -> limiter.checkAllowed(EMAIL, "198.51.100.1"))
			.isInstanceOf(TooManyRequestsException.class)
			.extracting(ex -> ((TooManyRequestsException) ex).getRetryAfter())
			.isEqualTo(LoginAttemptLimiter.WINDOW);
		assertAllowed("other@example.com", "198.51.100.1");
	}

	@Test
	void retryAfterShrinksAsTheWindowGoesBy() {
		fail(EMAIL, IP, LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL);
		clock.advance(Duration.ofMinutes(10));

		assertThatThrownBy(() -> limiter.checkAllowed(EMAIL, IP)).isInstanceOf(TooManyRequestsException.class)
			.extracting(ex -> ((TooManyRequestsException) ex).getRetryAfter())
			.isEqualTo(Duration.ofMinutes(5));
	}

	@Test
	void allowsTheEmailAgainOnceTheWindowIsOver() {
		fail(EMAIL, IP, LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL);
		clock.advance(LoginAttemptLimiter.WINDOW);

		assertAllowed(EMAIL, IP);
	}

	@Test
	void blocksTheIpOnceItsLimitIsReachedWhateverTheEmail() {
		for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES_PER_IP; i++) {
			limiter.recordFailure("user" + i + "@example.com", IP);
		}

		assertThatThrownBy(() -> limiter.checkAllowed("new@example.com", IP))
			.isInstanceOf(TooManyRequestsException.class);
		assertAllowed("new@example.com", "198.51.100.1");
	}

	@Test
	void successClearsTheEmailCounterButNotTheIpCounter() {
		fail(EMAIL, IP, LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL - 1);
		limiter.recordSuccess(EMAIL);
		fail(EMAIL, IP, LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL - 1);

		assertAllowed(EMAIL, IP);
		for (int i = 2 * (LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL - 1); i < LoginAttemptLimiter.MAX_FAILURES_PER_IP; i++) {
			limiter.recordFailure("user" + i + "@example.com", IP);
		}
		assertThatThrownBy(() -> limiter.checkAllowed(EMAIL, IP)).isInstanceOf(TooManyRequestsException.class);
	}

	@Test
	void failuresInAnExpiredWindowStartANewOne() {
		fail(EMAIL, IP, LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL - 1);
		clock.advance(LoginAttemptLimiter.WINDOW);
		fail(EMAIL, IP, LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL - 1);

		assertAllowed(EMAIL, IP);
	}

	@Test
	void checkingDoesNotCountAsAFailure() {
		fail(EMAIL, IP, LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL - 1);
		for (int i = 0; i < 10; i++) {
			assertAllowed(EMAIL, IP);
		}
	}

	private void fail(String email, String ip, int times) {
		for (int i = 0; i < times; i++) {
			limiter.recordFailure(email, ip);
		}
	}

	private void assertAllowed(String email, String ip) {
		assertThatCode(() -> limiter.checkAllowed(email, ip)).doesNotThrowAnyException();
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			throw new UnsupportedOperationException();
		}

	}

}
