/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.gate;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.lang.Math.abs;


/**
 * Secret policy.
 *
 * <p>Verifies secret conformance to well-formedness rules.</p>
 */
@FunctionalInterface public interface Policy {

	/**
	 * Retrieves the default policy factory.
	 *
	 * @return the default policy factory, which throws an exception reporting the tool as undefined
	 */
	public static Supplier<Policy> policy() {
		return () -> { throw new IllegalStateException("undefined policy tool"); };
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Policy all(final Policy... policies) {
		return (handle, secret) -> Arrays.stream(policies).allMatch(policy -> policy.verify(handle, secret));
	}

	public static Policy any(final Policy... policies) {
		return (handle, secret) -> Arrays.stream(policies).anyMatch(policy -> policy.verify(handle, secret));
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
		return characters(c -> Arrays.stream(blocks).anyMatch(block -> block.equals(Character.UnicodeBlock.of(c))));
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

	/**
	 * Verifies a secret.
	 *
	 * @param handle a handle identifying the user the secret belongs to
	 * @param secret the secret to be verified
	 *
	 * @return {@code true}, if {@code secret} conforms to the well-formedness rules defined by this policy; {@code
	 * false}, otherwise
	 *
	 * @throws NullPointerException if either {@code handle} or {@code secret} is null
	 */
	public boolean verify(final String handle, final String secret);

}
