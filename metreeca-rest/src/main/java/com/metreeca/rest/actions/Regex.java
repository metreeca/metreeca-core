/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest.actions;

import com.metreeca.rest.Xtream;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.*;

/**
 * Regular expression-based string processing.
 *
 * <p>Maps strings to values produced by a function taking as argument a string-targeted regular expression
 * processor.</p>
 *
 * @param <R> the type of the value returned by the processing action
 */
public final class Regex<R> implements Function<String, R> {

	private static final Map<String, Pattern> patterns=new ConcurrentHashMap<>(); // pattern cache


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Function<Processor, R> query;

	/**
	 * Creates regular expression-based string processing action.
	 *
	 * @param query a function taking as argument a processor and returning a value
	 *
	 * @throws NullPointerException if {@code query} is null
	 */
	public Regex(final Function<Processor, R> query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		this.query=query;
	}


	@Override public R apply(final String string) {
		return query.apply(new Processor(string));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Regular expression processor.
	 *
	 * <p>Applies regular expression to strings.</p>
	 */
	public static final class Processor {

		private final String string;


		/**
		 * Creates a new regular expression processor.
		 *
		 * @param string the target string for the processor
		 *
		 * @throws NullPointerException if {@code string} is null
		 */
		public Processor(final String string) {

			if ( string == null ) {
				throw new NullPointerException("null string");
			}

			this.string=string;
		}


		/**
		 * Retrieves the target string.
		 *
		 * @return the target string of this processor.
		 */
		public String string() {
			return string;
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		/**
		 * Checks if the target string contains a pattern.
		 *
		 * @param pattern the pattern to be matched against the target string
		 *
		 * @return {@code true} if the target string of this action contains a substring matching {@code pattern}
		 *
		 * @throws NullPointerException if {@code pattern} is null
		 */
		public boolean find(final String pattern) {

			if ( pattern == null ) {
				throw new NullPointerException("null pattern");
			}

			return matcher(pattern).find();
		}

		/**
		 * Checks if the target string matches a pattern.
		 *
		 * @param pattern the pattern to be matched against the target string
		 *
		 * @return {@code true} if the target string of this action matches {@code pattern}
		 *
		 * @throws NullPointerException if {@code pattern} is null
		 */
		public boolean matches(final String pattern) {

			if ( pattern == null ) {
				throw new NullPointerException("null pattern");
			}

			return matcher(pattern).matches();
		}


		/**
		 * Retrieves a matching group from the target string.
		 *
		 * @param pattern the pattern to be matched against the target string
		 * @param group   the index of the group to be retrieved
		 *
		 * @return an optional string containing the section of the target string matching {@code group}, if the target
		 * string matches {@code pattern}; an empty optional, otherwise
		 *
		 * @throws NullPointerException     if {@code pattern} is null
		 * @throws IllegalArgumentException if {@code group} is negative or {@code pattern} doesn't contain a
		 *                                  corresponding group
		 */
		public Optional<String> group(final String pattern, final int group) {

			if ( pattern == null ) {
				throw new NullPointerException("null pattern");
			}

			if ( group < 0 ) {
				throw new IllegalArgumentException("negative group index");
			}

			return result(pattern).map(result -> result.group(group));
		}

		/**
		 * Retrieves all matching groups from the target string.
		 *
		 * @param pattern the pattern to be matched against the target string
		 * @param group   the index of the group to be retrieved
		 *
		 * @return a stream of strings containing all the sections of the target string matching {@code group}
		 *
		 * @throws NullPointerException     if {@code pattern} is null
		 * @throws IllegalArgumentException if {@code group} is negative or {@code pattern} doesn't contain a
		 *                                  corresponding group
		 */
		public Xtream<String> groups(final String pattern, final int group) {

			if ( pattern == null ) {
				throw new NullPointerException("null pattern");
			}

			if ( group < 0 ) {
				throw new IllegalArgumentException("negative group index");
			}

			return results(pattern).map(result -> result.group(group));
		}


		/**
		 * Retrieves a match result from the target string.
		 *
		 * @param pattern the pattern to be matched against the target string
		 *
		 * @return an optional match result describing the match against the target string, if the target string matches
		 * {@code pattern}; an empty optional, otherwise
		 *
		 * @throws NullPointerException if {@code pattern} is null
		 */
		public Optional<MatchResult> result(final String pattern) {

			if ( pattern == null ) {
				throw new NullPointerException("null pattern");
			}

			return Optional
					.of(matcher(pattern))
					.filter(Matcher::matches)
					.map(Matcher::toMatchResult);
		}

		/**
		 * Retrieves all match results from the target string.
		 *
		 * @param pattern the pattern to be matched against the target string
		 *
		 * @return a stream of match result describing the matches against the target string
		 *
		 * @throws NullPointerException if {@code pattern} is null
		 */
		public Xtream<MatchResult> results(final String pattern) {

			if ( pattern == null ) {
				throw new NullPointerException("null pattern");
			}

			final Collection<MatchResult> results=new ArrayList<>();

			for (final Matcher matcher=matcher(pattern); matcher.find(); ) {
				results.add(matcher.toMatchResult());
			}

			return Xtream.from(results.stream());

		}


		/**
		 * Replaces all occurrences of a pattern in the target string.
		 *
		 * @param pattern     the pattern to be matched against the target string
		 * @param replacement the replacement string for the matches of {@code pattern}
		 *
		 * @return a string with all the occurences of {@code pattern} replaced according to the {@code replacement}
		 * string
		 *
		 * @throws NullPointerException if either {@code pattern} or {@code replacement} is null
		 */
		public String replace(final String pattern, final String replacement) {

			if ( pattern == null ) {
				throw new NullPointerException("null pattern");
			}

			if ( replacement == null ) {
				throw new NullPointerException("null replacement");
			}

			return matcher(pattern).replaceAll(replacement);
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		private Matcher matcher(final String pattern) {
			return patterns.computeIfAbsent(pattern, Pattern::compile).matcher(string);
		}

	}

}
