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


import org.eclipse.rdf4j.model.Value;

import java.util.*;

import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Strings.indent;
import static com.metreeca.form.things.Values.format;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;


/**
 * Shape value validation report.
 */
public final class Frame {

	@SafeVarargs public static  Frame frame(final Value value, final Map.Entry<Shift, Focus>... fields) {
		return frame(value, map(fields));
	}

	public static  Frame frame(final Value value, final Map<Shift, Focus> fields) {
		return new Frame(value, fields);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Value value;
	private final Set<Issue> issues;
	private final Map<Shift, Focus> fields;


	private Frame(final Value value, final Map<Shift, Focus> fields) {

		if ( value == null ) {
			throw new NullPointerException("null value");
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
		this.issues=new LinkedHashSet<>(); // !!!
		this.fields=new LinkedHashMap<>(fields);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Value getValue() {
		return value;
	}

	public Set<Issue> getIssues() {
		return unmodifiableSet(issues);
	}

	public Map<Shift, Focus> getFields() {
		return unmodifiableMap(fields);
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
