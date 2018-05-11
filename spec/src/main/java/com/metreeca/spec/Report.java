/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec;

import com.metreeca.spec.shifts.Step;
import com.metreeca.spec.things.Maps;
import com.metreeca.spec.things.Sets;

import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.spec.things.Values.statement;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;


/**
 * Shape validation report.
 */
public final class Report {

	private static final Report empty=new Report(Sets.set(), Sets.set());


	public static Report trace() {
		return empty;
	}

	public static Report trace(final Issue... issues) {
		return new Report(Sets.set(issues), Sets.set());
	}

	@SafeVarargs public static Report trace(final Collection<Issue> issues, final Frame<Report>... frames) {
		return new Report(issues, Sets.set(frames));
	}

	public static Report trace(final Collection<Issue> issues, final Collection<Frame<Report>> frames) {
		return new Report(issues, frames);
	}


	private final Set<Issue> issues;
	private final Set<Frame<Report>> frames;


	public Report(final Collection<Issue> issues, final Collection<Frame<Report>> frames) {

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


	public Set<Issue> getIssues() {
		return unmodifiableSet(issues);
	}

	public Set<Frame<Report>> getFrames() {
		return unmodifiableSet(frames);
	}


	/**
	 * Tests if the overall severity of this trace node reaches an expected level.
	 *
	 * @param limit the expected severity level
	 *
	 * @return {@code true} if at least a issue or a frame reaches the severity {@code limit}
	 */
	public boolean assess(final Issue.Level limit) {

		if ( limit == null ) {
			throw new NullPointerException("null limit");
		}

		return issues.stream()
				.anyMatch(issue -> issue.getLevel().compareTo(limit) >= 0) || frames.stream()
				.flatMap(frame -> frame.getSlots().values().stream())
				.anyMatch(trace -> trace.assess(limit));
	}

	/**
	 * Removes all issues and frames under a given issue {@linkplain Issue.Level severity level}.
	 *
	 * @param limit the minimum severity level for retained issues and frames
	 *
	 * @return an optional pruned trace retaining only issues and frames from this trace with severity greater or equal
	 * to {@code limit}; an empty optional if no issue or frame in this trace reaches the severity {@code limit}
	 */
	public Optional<Report> prune(final Issue.Level limit) {

		if ( limit == null ) {
			throw new NullPointerException("null limit");
		}

		final Set<Issue> issues=this.issues.stream()
				.filter(issue -> issue.getLevel().compareTo(limit) >= 0)
				.collect(toCollection(LinkedHashSet::new));

		final Set<Frame<Report>> frames=this.frames.stream()
				.map((frame) -> prune(frame, limit))
				.filter(Optional::isPresent).map(Optional::get)
				.collect(toCollection(LinkedHashSet::new));

		return issues.isEmpty() && frames.isEmpty() ? Optional.empty() : Optional.of(trace(issues, frames));
	}

	/**
	 * Computes the statement outline of this trace node.
	 *
	 * @return a collection of statements recursively generated from {@linkplain Frame frames} in this trace node
	 */
	public Collection<Statement> outline() {
		return frames.stream()
				.flatMap(this::outline)
				.collect(toCollection(LinkedHashSet::new));
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Report
				&& issues.equals(((Report)object).issues)
				&& frames.equals(((Report)object).frames);
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

	private Optional<Frame<Report>> prune(final Frame<Report> frame, final Issue.Level limit) {

		final Value value=frame.getValue();

		final List<Map.Entry<Step, Report>> slots=frame.getSlots().entrySet().stream()
				.map(slot -> slot.getValue().prune(limit).map(trace -> Maps.entry(slot.getKey(), trace)))
				.filter(Optional::isPresent).map(Optional::get)
				.collect(toList());

		return slots.isEmpty() ? Optional.empty() : Optional.of(new Frame<>(value, Maps.map(slots)));
	}

	private Stream<Statement> outline(final Frame<Report> frame) {

		final Value source=frame.getValue();

		return frame.getSlots().entrySet().stream().flatMap(slot -> {

			final Step step=slot.getKey();

			final IRI iri=step.getIRI();
			final boolean inverse=step.isInverse();

			final Stream<Value> targets=slot.getValue().frames.stream().map(Frame::getValue);

			return Stream.concat(

					!inverse && source instanceof Resource ? targets

							.map(target -> statement((Resource)source, iri, target))

							: inverse ? targets

							.filter(target -> target instanceof Resource)
							.map(target -> statement((Resource)target, iri, source))

							: Stream.empty(),

					slot.getValue().frames.stream().flatMap(this::outline)

			);
		});
	}

}
