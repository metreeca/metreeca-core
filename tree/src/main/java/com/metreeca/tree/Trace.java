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

package com.metreeca.tree;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;


/**
 * Shape validation trace.
 */
public final class Trace {

	private static final Trace EmptyTrace=new Trace(Stream.empty(), Stream.empty());


	public static Trace trace() {
		return EmptyTrace;
	}

	public static Trace trace(final Trace... traces) {return trace(asList(traces));}

	public static Trace trace(final Collection<Trace> traces) {

		if ( traces == null || traces.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null traces");
		}

		return new Trace(
				traces.stream().flatMap(trace -> trace.issues.entrySet().stream()),
				traces.stream().flatMap(trace -> trace.fields.entrySet().stream())
		);
	}

	public static Trace trace(final String issue, final Object... values) {
		return new Trace(singletonMap(issue, (Collection<Object>)asList(values)).entrySet().stream(), Stream.empty());
	}

	public static Trace trace(final Map<String, Collection<Object>> issues) {
		return new Trace(issues.entrySet().stream(), Stream.empty());
	}

	public static Trace trace(final Map<String, Collection<Object>> issues, final Map<String, Trace> fields) {
		return new Trace(issues.entrySet().stream(), fields.entrySet().stream());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<String, Collection<Object>> issues;
	private final Map<String, Trace> fields;


	private Trace(
			final Stream<Map.Entry<String, Collection<Object>>> issues,
			final Stream<Map.Entry<String, Trace>> fields
	) {

		this.issues=issues
				.filter(issue -> !issue.getKey().isEmpty())
				.collect(toMap(
						Map.Entry::getKey, entry -> unmodifiableSet(new LinkedHashSet<>(entry.getValue())),
						(x, y) -> Stream.of(x, y).flatMap(Collection::stream).collect(toCollection(LinkedHashSet::new)),
						LinkedHashMap::new
				));

		this.fields=fields
				.filter(field -> !field.getKey().isEmpty())
				.filter(field -> !field.getValue().isEmpty())
				.collect(toMap(
						Map.Entry::getKey, Map.Entry::getValue,
						(x, y) -> trace(x, y),
						LinkedHashMap::new
				));
	}


	public boolean isEmpty() {
		return issues.isEmpty() && fields.isEmpty();
	}


	public Map<String, Collection<Object>> getIssues() {
		return unmodifiableMap(issues);
	}

	public Map<String, Trace> getFields() {
		return unmodifiableMap(fields);
	}


	@Override public String toString() {
		return String.format("{\n\tissue: %s\n\tfields: %s\n}", issues, fields);
	}

}
