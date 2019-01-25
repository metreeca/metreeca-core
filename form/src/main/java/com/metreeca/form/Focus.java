/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.metreeca.form.Frame.frame;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Sets.union;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.*;


/**
 * Shape focus validation report.
 */
public final class Focus {

	private static final Focus empty=new Focus(set(), set());


	public static Focus focus() {
		return empty;
	}

	public static Focus focus(final Issue... issues) {
		return new Focus(set(issues), set());
	}

	public static Focus focus(final Collection<Issue> issues, final Frame... frames) {
		return new Focus(issues, set(frames));
	}

	public static Focus focus(final Collection<Issue> issues, final Collection<Frame> frames) {
		return new Focus(issues, frames);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Set<Issue> issues;
	private final Set<Frame> frames;


	private Focus(final Collection<Issue> issues, final Collection<Frame> frames) {

		if ( issues == null ) {
			throw new NullPointerException("null issues");
		}

		if ( issues.contains(null) ) {
			throw new NullPointerException("null issue");
		}

		if ( frames == null ) {
			throw new NullPointerException("null frames");
		}

		if ( frames.contains(null) ) {
			throw new NullPointerException("null frame");
		}

		this.issues=new LinkedHashSet<>(issues);
		this.frames=new LinkedHashSet<>(frames);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Set<Issue> getIssues() {
		return unmodifiableSet(issues);
	}

	public Set<Frame> getFrames() {
		return unmodifiableSet(frames);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Focus merge(final Focus focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus report");
		}

		final Set<Issue> issues=union(getIssues(), focus.getIssues());
		final Collection<Frame> frames=frames(union(getFrames(), focus.getFrames()), reducing(focus(), Focus::merge));

		return focus(issues, frames);
	}

	/**
	 * Tests if the overall severity of this report reaches an expected level.
	 *
	 * @param limit the expected severity level
	 *
	 * @return {@code true} if at least a issue or a frame reaches the severity {@code limit}
	 *
	 * @throws NullPointerException if {@code limit} is null
	 */
	public boolean assess(final Issue.Level limit) {

		if ( limit == null ) {
			throw new NullPointerException("null limit");
		}

		return issues.stream().anyMatch(issue -> issue.assess(limit))
				|| frames.stream().anyMatch(trace -> trace.assess(limit));
	}

	/**
	 * Removes all issues and frames under a given issue {@linkplain Issue.Level severity level}.
	 *
	 * @param limit the minimum severity level for retained issues and frames
	 *
	 * @return an optional pruned focus report retaining only issues and frames with severity greater or equal to {@code
	 * limit}; an empty optional if no issue or frame in this focus record reaches the severity {@code limit}
	 */
	public Optional<Focus> prune(final Issue.Level limit) {

		if ( limit == null ) {
			throw new NullPointerException("null limit");
		}

		final Collection<Issue> issues=this.issues.stream()
				.filter(issue -> issue.assess(limit))
				.collect(toList());

		final Set<Frame> frames=this.frames.stream()
				.map(frame -> frame.prune(limit))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(toCollection(LinkedHashSet::new));

		return issues.isEmpty() && frames.isEmpty() ? Optional.empty() : Optional.of(focus(issues, frames));
	}

	/**
	 * Computes the statement outline of this report.
	 *
	 * @return a stream of statements recursively generated from {@linkplain Frame frames} in this report
	 */
	public Stream<Statement> outline() {
		return frames.stream().flatMap(Frame::outline);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Focus
				&& issues.equals(((Focus)object).issues)
				&& frames.equals(((Focus)object).frames);
	}

	@Override public int hashCode() {
		return issues.hashCode()^frames.hashCode();
	}

	@Override public String toString() {
		return issues.stream().map(Issue::toString).collect(joining("\n\n"))
				+(issues.isEmpty() || frames.isEmpty() ? "" : "\n\n")
				+frames.stream().map(Frame::toString).collect(joining("\n\n"));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Merges a collection of frames.
	 *
	 * @param frames    the frames to be merged
	 * @param collector a collector transforming a stream of values into a merged value (usually a {@linkplain
	 *                  Collectors#reducing} collector)
	 *
	 * @return a merged collection of frames where each frame value appears only once
	 */
	private Collection<Frame> frames(final Collection<Frame> frames, final Collector<Focus, ?, Focus> collector) {

		// field maps merge operator

		final BinaryOperator<Map<IRI, Focus>> operator=(x, y) -> Stream.of(x, y)
				.flatMap(field -> field.entrySet().stream())
				.collect(groupingBy(Map.Entry::getKey, LinkedHashMap::new,
						mapping(Map.Entry::getValue, collector)));

		// group field maps by frame value and merge

		final Map<Value, Map<IRI, Focus>> map=frames.stream().collect(
				groupingBy(Frame::getValue, LinkedHashMap::new,
						mapping(Frame::getFields, reducing(map(), operator))));

		// convert back to frames

		return map.entrySet().stream()
				.map(e -> frame(e.getKey(), e.getValue()))
				.collect(toList());
	}

}
