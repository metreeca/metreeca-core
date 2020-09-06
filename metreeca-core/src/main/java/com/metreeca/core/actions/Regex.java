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

package com.metreeca.core.actions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.*;
import java.util.stream.Stream;


/**
 * Regular expression matching.
 *
 * <p>Applies regular expression to strings.</p>
 */
public final class Regex {

    private static final Map<String, Pattern> patterns=new ConcurrentHashMap<>(); // pattern cache


    /**
     * Creates a regex-based function.
     *
     * @param query a function taking as argument a regular expression matching action and returning a value
     * @param <R>   the type of the value returned by {@code query}
     *
     * @return a function taking a string as argument and returning the value produced by applying {@code query} to a
     * regular expression matching action targeting the argument
     *
     * @throws NullPointerException if {@code query} is null
     */
    public static <R> Function<String, R> Regex(final Function<Regex, R> query) {

        if ( query == null ) {
            throw new NullPointerException("null query");
        }

        return string -> query.apply(new Regex(string));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final String string;


    /**
     * Creates a new regular expression matching action.
     *
     * @param string the target string for the action
     *
     * @throws NullPointerException if {@code string} is null
     */
    public Regex(final String string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        this.string=string;
    }


    /**
     * Retrieves the target string.
     *
     * @return the target string of this regular expression matching action.
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
    public Stream<String> groups(final String pattern, final int group) {

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
    public Stream<MatchResult> results(final String pattern) {

        if ( pattern == null ) {
            throw new NullPointerException("null pattern");
        }

        final Collection<MatchResult> results=new ArrayList<>();

        for (final Matcher matcher=matcher(pattern); matcher.find(); ) {
            results.add(matcher.toMatchResult());
        }

        return results.stream();

    }

    /**
     * Replaces all occurences of a pattern in the target string.
     *
     * @param pattern     the pattern to be matched against the target string
     * @param replacement the replacement string for the matches of {@code pattern}
     *
     * @return a string with all the occurences of {@code pattern} replaced according to the {@code replacement} string
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
