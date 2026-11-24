package com.tao.fenuasmash.common.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Keys to compare or search texts regardless of case, accents and extra spaces. */
public final class TextKeys {

	private static final Pattern MARKS = Pattern.compile("\\p{M}+");

	private static final Pattern SPACES = Pattern.compile("\\s+");

	private TextKeys() {
	}

	/** "  Rémi   LE Goff " gives "remi le goff". */
	public static String of(String text) {
		String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
		String plain = MARKS.matcher(decomposed).replaceAll("");
		return SPACES.matcher(plain.strip()).replaceAll(" ").toLowerCase(Locale.ROOT);
	}

}
