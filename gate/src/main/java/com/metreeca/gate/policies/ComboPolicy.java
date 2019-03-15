/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.gate.policies;

import com.metreeca.gate.Policy;

import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static java.util.Arrays.stream;


/**
 * Combo policy.
 *
 * <p>Verifies secret conformance to a set of delegated policies.</p>
 */
public final class ComboPolicy implements Policy {

	public static Policy all(final Policy... policies) {
		return (handle, secret) -> stream(policies).allMatch(policy -> policy.verify(handle, secret));
	}

	public static Policy any(final Policy... policies) {
		return (handle, secret) -> stream(policies).anyMatch(policy -> policy.verify(handle, secret));
	}


	public static Policy no(final BiFunction<String, String, Long> counter) {
		return maximum(0, counter);
	}

	public static Policy contains(final BiFunction<String, String, Long> counter) {
		return minimum(1, counter);
	}

	public static Policy minimum(final long minimum, final BiFunction<String, String, Long> counter) {
		return between(minimum, Long.MAX_VALUE, counter);
	}

	public static Policy maximum(final long maximum, final BiFunction<String, String, Long> counter) {
		return between(0, maximum, counter);
	}

	public static Policy between(final long minimum, final long maximum, final BiFunction<String, String, Long> counter) {
		return (handle, secret) -> {

			final long count=counter.apply(handle, secret);

			return minimum <= count && count <= maximum;
		};
	}


	public static Policy only(final BiFunction<String, String, Long> counter) {
		return (handle, secret) -> counter.apply(handle, secret) == secret.length();
	}


	public static BiFunction<String, String, Long> letter() {
		return characters(Character::isLetter);
	}

	public static BiFunction<String, String, Long> uppercases() {
		return characters(Character::isUpperCase);
	}

	public static BiFunction<String, String, Long> lowercases() {
		return characters(Character::isLowerCase);
	}

	public static BiFunction<String, String, Long> digits() {
		return characters(Character::isDigit);
	}

	public static BiFunction<String, String, Long> controls() {
		return characters(Character::isISOControl);

	}

	public static BiFunction<String, String, Long> specials() {
		return characters(c -> !Character.isLetterOrDigit(c) && !Character.isISOControl(c));
	}


	public static BiFunction<String, String, Long> block(final Character.UnicodeBlock... blocks) {
		return characters(c -> stream(blocks).anyMatch(block -> block.equals(Character.UnicodeBlock.of(c))));
	}

	public static BiFunction<String, String, Long> script(final Character.UnicodeScript... scripts) {
		return characters(c -> stream(scripts).anyMatch(script -> script == Character.UnicodeScript.of(c)));
	}


	public static BiFunction<String, String, Long> characters() {
		return characters(character -> true);
	}

	public static BiFunction<String, String, Long> characters(final IntPredicate classifier) {
		return (handle, secret) -> secret.chars().filter(classifier).count();
	}


	public static BiFunction<String, String, Long> stopwords() {
		return (handle, secret) -> Pattern.compile("\\W+")
				.splitAsStream(handle.toUpperCase(Locale.ROOT))
				.filter(s -> !s.isEmpty())
				.filter(s -> secret.toUpperCase(Locale.ROOT).contains(s))
				.count();
	}

	public static BiFunction<String, String, Long> sequences(final int length) {
		return (handle, secret) -> {

			long count=0L;

			for (int s=0, e=1, l=secret.length(); e <= l; ++e) {
				if ( e == l || abs(secret.charAt(e)-secret.charAt(e-1)) > 1 ) {

					if ( e-s >= length) { ++ count; }

					s=e;
				}

			}

			return count;

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Policy delegate;


	/**
	 * Creates a new combo policy
	 * @param policies the set of delegated policies veriried secrets must conform to
	 *
	 * @throws NullPointerException if {@code policies} is null or contains null values
	 */
	public ComboPolicy(final Policy... policies) {

		if ( policies == null || stream(policies).anyMatch(Objects::isNull)) {
			throw new NullPointerException("null policies");
		}

		this.delegate=all(policies);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean verify(final String handle, final String secret) {
		return delegate.verify(handle, secret);
	}

}
