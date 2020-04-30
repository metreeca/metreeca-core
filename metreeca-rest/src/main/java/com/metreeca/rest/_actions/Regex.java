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

package com.metreeca.rest._actions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


/**
 * {@link Pattern}
 * {@link Character}
 */
public final class Regex {

	private static final Map<String, Pattern> patterns=new ConcurrentHashMap<>();


	public static Predicate<String> Matches(final String regex) {

		if ( regex == null ) {
			throw new NullPointerException("null regex");
		}

		return string -> new Regex(string).matches(regex);
	}


	public static Function<String, Stream<String>> Matches(final String regex, final String group) {

		if ( regex == null ) {
			throw new NullPointerException("null regex");
		}

		if ( group == null ) {
			throw new NullPointerException("null group");
		}

		return string -> new Regex(string).matches(regex, group);
	}

	public static Function<String, String> Replace(final String regex, final String replacement) {

		if ( regex == null ) {
			throw new NullPointerException("null regex");
		}

		if ( replacement == null ) {
			throw new NullPointerException("null replacement");
		}

		return string -> new Regex(string).replace(regex, replacement);
	}


	public static <R> Function<String, R> Query(final Function<Regex, R> query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return string -> query.apply(new Regex(string));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String string;


	public Regex(final String string) {

		if ( string == null ) {
			throw new NullPointerException("null string");
		}

		this.string=string;
	}


	public String string() {
		return string;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean matches(final String regex) {

		if ( regex == null ) {
			throw new NullPointerException("null regex");
		}

		return evaluate(regex, Matcher::find);
	}


	public Optional<String> match(final String regex, final String group) {

		if ( regex == null ) {
			throw new NullPointerException("null regex");
		}

		if ( group == null ) {
			throw new NullPointerException("null group");
		}

		return match(regex, matcher -> matcher.group(group));
	}

	public <R> Optional<R> match(final String regex, final Function <Matcher, R> mapper) {

		if ( regex == null ) {
			throw new NullPointerException("null regex");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return evaluate(regex, matcher -> Optional.ofNullable(matcher.matches()? mapper.apply(matcher) : null));
	}


	public Stream<String> matches(final String regex, final String group) {

		if ( regex == null ) {
			throw new NullPointerException("null regex");
		}

		if ( group == null ) {
			throw new NullPointerException("null group");
		}

		return matches(regex, matcher -> matcher.group(group));
	}

	public  <R> Stream<R> matches(final String regex, final Function <Matcher, R> mapper) {

		if ( regex == null ) {
			throw new NullPointerException("null regex");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return evaluate(regex, matcher -> {

			final Collection<R> matches=new ArrayList<>();

			while ( matcher.find() ) {
				matches.add(mapper.apply(matcher));
			}

			return matches.stream().filter(Objects::nonNull);

		});
	}


	public  String replace(final String regex, final String replacement) {

		if ( regex == null ) {
			throw new NullPointerException("null regex");
		}

		if ( replacement == null ) {
			throw new NullPointerException("null replacement");
		}

		return evaluate(regex, matcher -> matcher.replaceAll(replacement));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public <R> R evaluate(final String regex, final Function<Matcher, R> query) {

		if ( regex == null ) {
			throw new NullPointerException("null regex");
		}

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return query.apply(patterns.computeIfAbsent(regex, Pattern::compile).matcher(string));
	}

}
