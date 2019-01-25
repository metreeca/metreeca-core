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


import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Strings.indent;
import static com.metreeca.form.things.Values.*;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;


/**
 * Shape value validation report.
 */
public final class Frame {

	@SafeVarargs public static Frame frame(final Value value, final Map.Entry<IRI, Focus>... fields) {
		return frame(value, map(fields));
	}

	public static Frame frame(final Value value, final Map<IRI, Focus> fields) {
		return frame(value, set(), fields);
	}

	public static Frame frame(final Value value, final Collection<Issue> issues) {
		return new Frame(value, issues, map());
	}

	public static Frame frame(final Value value, final Collection<Issue> issues, final Map<IRI, Focus> fields) {
		return new Frame(value, issues, fields);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Value value;
	private final Set<Issue> issues;
	private final Map<IRI, Focus> fields;


	private Frame(final Value value, final Collection<Issue> issues, final Map<IRI, Focus> fields) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		if ( issues == null ) {
			throw new NullPointerException("null issues");
		}

		if ( issues.contains(null) ) {
			throw new NullPointerException("null issue");
		}

		if ( fields == null ) {
			throw new NullPointerException("null fields");
		}

		if ( fields.containsKey(null) ) {
			throw new NullPointerException("null field IRI");
		}

		if ( fields.containsValue(null) ) {
			throw new NullPointerException("null field focus report");
		}

		this.value=value;
		this.issues=new LinkedHashSet<>(issues);
		this.fields=new LinkedHashMap<>(fields);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Value getValue() {
		return value;
	}

	public Set<Issue> getIssues() {
		return unmodifiableSet(issues);
	}

	public Map<IRI, Focus> getFields() {
		return unmodifiableMap(fields);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Tests if the overall severity of this report reaches an expected level.
	 *
	 * @param limit the expected severity level
	 *
	 * @return {@code true} if at least a issue or a field reaches the severity {@code limit}
	 *
	 * @throws NullPointerException if {@code limit} is null
	 */
	public boolean assess(final Issue.Level limit) {

		if ( limit == null ) {
			throw new NullPointerException("null limit");
		}

		return issues.stream().anyMatch(issue -> issue.assess(limit))
				|| fields.values().stream().anyMatch(trace -> trace.assess(limit));
	}

	/**
	 * Removes all issues and fields under a given issue {@linkplain Issue.Level severity level}.
	 *
	 * @param limit the minimum severity level for retained issues and fields
	 *
	 * @return an optional pruned frame retaining only issues and fields with severity greater or equal to {@code
	 * limit}; an empty optional if no issue or field in this report reaches the severity {@code limit}
	 */
	public Optional<Frame> prune(final Issue.Level limit) {

		if ( limit == null ) {
			throw new NullPointerException("null limit");
		}

		final List<Issue> issues=this.issues.stream()
				.filter(issue -> issue.assess(limit))
				.collect(toList());

		final List<Map.Entry<IRI, Focus>> fields=this.fields.entrySet().stream()
				.map(field -> field.getValue().prune(limit).map(focus -> entry(field.getKey(), focus)))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(toList()); // preserve ordering

		return issues.isEmpty() && fields.isEmpty() ? Optional.empty() : Optional.of(frame(value, issues, map(fields)));
	}


	public Stream<Statement> outline() {

		return fields.entrySet().stream().flatMap(field -> {

			final IRI iri=field.getKey();
			final boolean direct=direct(iri);

			final Focus focus=field.getValue();

			final Stream<Value> targets=focus.getFrames().stream().map(frame -> frame.value);

			return Stream.concat(

					direct && value instanceof Resource

							? targets.map(target -> statement((Resource)value, iri, target))

							: direct ? Stream.empty()

							: targets

							.filter(target -> target instanceof Resource)
							.map(target -> statement((Resource)target, inverse(iri), value)),

					focus.outline()

			);
		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Frame
				&& value.equals(((Frame)object).value)
				&& issues.equals(((Frame)object).issues)
				&& fields.equals(((Frame)object).fields);
	}

	@Override public int hashCode() {
		return value.hashCode()^issues.hashCode()^fields.hashCode();
	}

	@Override public String toString() {
		return format(value)+" {"+(fields.isEmpty() ? "" : indent(fields.entrySet().stream().map(e -> {

			final String edge=e.getKey().toString();
			final String value=e.getValue().toString();

			return edge+" :\n\n"+indent(value);

		}).collect(joining("\n\n", "\n\n", "\n\n"))))+"}";
	}

}
