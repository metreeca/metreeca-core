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

package com.metreeca.json;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static java.util.Collections.*;
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

	public static Trace trace(final Trace... traces) {
		return new Trace(
				Arrays.stream(traces).flatMap(trace -> trace.issues().stream()),
				Arrays.stream(traces).flatMap(trace -> trace.fields().entrySet().stream())
		);
	}

	public static Trace trace(final String issue) {
		return new Trace(Stream.of(issue), Stream.empty());
	}

	public static Trace trace(final Stream<String> issues) {
		return new Trace(issues, Stream.empty());
	}

	public static Trace trace(final String field, final Trace trace) {
		return new Trace(Stream.empty(), Stream.of(new AbstractMap.SimpleImmutableEntry<>(field, trace)));
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Collection<String> issues;
	private final Map<String, Trace> fields;


	private Trace(final Stream<String> issues, final Stream<Entry<String, Trace>> fields) {

		this.issues=issues
				.filter(issue -> !issue.isEmpty())
				.collect(toCollection(LinkedHashSet::new));

		this.fields=fields
				.filter(field -> !field.getValue().empty())
				.collect(toMap(Entry::getKey, Entry::getValue, Trace::trace, LinkedHashMap::new));
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean empty() {
		return issues.isEmpty() && fields.isEmpty();
	}


	public Collection<String> issues() {
		return unmodifiableCollection(issues);
	}

	public Map<String, Trace> fields() {
		return unmodifiableMap(fields);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public JsonObject toJSON() {

		final JsonObjectBuilder builder=Json.createObjectBuilder();

		if ( !issues().isEmpty() ) {
			builder.add("", Json.createArrayBuilder(issues));
		}

		fields().forEach((label, nested) -> {

			if ( !nested.empty() ) {
				builder.add(label, nested.toJSON());
			}

		});

		return builder.build();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String toString() {
		try ( final StringWriter writer=new StringWriter() ) {

			Json
					.createWriterFactory(singletonMap(JsonGenerator.PRETTY_PRINTING, true))
					.createWriter(writer)
					.write(toJSON());

			return writer.toString();

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}

}
