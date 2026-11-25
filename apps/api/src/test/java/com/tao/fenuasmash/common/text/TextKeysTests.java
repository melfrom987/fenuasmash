package com.tao.fenuasmash.common.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextKeysTests {

	@Test
	void ignoresCaseAccentsAndExtraSpaces() {
		assertThat(TextKeys.of("  Rémi   LE Goff ")).isEqualTo("remi le goff");
		assertThat(TextKeys.of("Ōkini Tahiti")).isEqualTo(TextKeys.of("okini TAHITI"));
		assertThat(TextKeys.of("Faa'a Badminton Club")).isEqualTo("faa'a badminton club");
	}

	@Test
	void treatsDashesAsSpaces() {
		assertThat(TextKeys.of("Ah-Sing")).isEqualTo(TextKeys.of("ah sing"));
		assertThat(TextKeys.of("Jean – Paul")).isEqualTo("jean paul");
	}

}
