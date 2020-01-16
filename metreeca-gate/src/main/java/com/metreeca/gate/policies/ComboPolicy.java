/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.gate.policies;

import com.metreeca.gate.Policy;

import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Stream;

import static java.lang.Math.abs;
import static java.util.Arrays.stream;


/**
 * Combo policy.
 *
 * <p>Delegates secret conformance verification to a {@linkplain #delegate(Policy) delegate} policy, possibly assembled
 * as a combination of other policies.</p>
 */
public abstract class ComboPolicy implements Policy {

	public static Policy all(final Policy... policies) {
		return (user, secret) -> stream(policies).allMatch(policy -> policy.verify(user, secret));
	}

	public static Policy any(final Policy... policies) {
		return (user, secret) -> stream(policies).anyMatch(policy -> policy.verify(user, secret));
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
		return (user, secret) -> {

			final long count=counter.apply(user, secret);

			return minimum <= count && count <= maximum;
		};
	}


	public static Policy only(final BiFunction<String, String, Long> counter) {
		return (user, secret) -> counter.apply(user, secret) == secret.length();
	}


	public static BiFunction<String, String, Long> letters() {
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


	public static BiFunction<String, String, Long> blocks(final Character.UnicodeBlock... blocks) {
		return characters(c -> stream(blocks).anyMatch(block -> block.equals(Character.UnicodeBlock.of(c))));
	}

	public static BiFunction<String, String, Long> scripts(final Character.UnicodeScript... scripts) {
		return characters(c -> stream(scripts).anyMatch(script -> script == Character.UnicodeScript.of(c)));
	}


	public static BiFunction<String, String, Long> characters() {
		return characters(character -> true);
	}

	public static BiFunction<String, String, Long> characters(final IntPredicate classifier) {
		return (user, secret) -> secret.chars().filter(classifier).count();
	}


	public static BiFunction<String, String, Long> stopwords(final Function<String, Stream<String>> generator) {

		if ( generator == null ) {
			throw new NullPointerException("null generator");
		}

		return (user, secret) -> generator
				.apply(user)
				.filter(s -> !s.isEmpty())
				.filter(s -> secret.toUpperCase(Locale.ROOT).contains(s.toUpperCase(Locale.ROOT)))
				.count();
	}

	public static BiFunction<String, String, Long> sequences(final int length) {

		if ( length < 0 ) {
			throw new IllegalArgumentException("negative length");
		}

		return (user, secret) -> {

			long count=0L;

			for (int s=0, e=1, l=secret.length(); e <= l; ++e) {
				if ( e == l || abs(secret.charAt(e)-secret.charAt(e-1)) > 1 ) {

					if ( e-s >= length ) { ++count; }

					s=e;
				}

			}

			return count;

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Policy delegate;


	/**
	 * Retrieves the delegate policy.
	 *
	 * @return the policy credential validation is delegated to
	 *
	 * @throws IllegalStateException if the delegate policy wasn't {@linkplain #delegate(Policy) configured}
	 */
	protected Policy delegate() {

		if ( delegate == null ) {
			throw new IllegalStateException("undefined delegate");
		}

		return delegate;
	}

	/**
	 * Configures the delegate policy.
	 *
	 * @param delegate the policy credential validation is delegated to
	 *
	 * @return this combo policy
	 *
	 * @throws NullPointerException if {@code delegate} is null
	 */
	protected ComboPolicy delegate(final Policy delegate) {

		if ( delegate == null ) {
			throw new NullPointerException("null delegate");
		}

		this.delegate=delegate;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean verify(final String user, final String secret) {
		return delegate().verify(user, secret);
	}

}
