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

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


/**
 * Shape value validation trace.
 */
public final class Trace {

	private static final Trace EmptyTrace=new Trace(emptySet(), emptyMap());


	public static Trace trace() {
		return EmptyTrace;
	}

	public static Trace trace(final String... issues) {
		return new Trace(asList(issues), emptyMap());
	}

	public static Trace trace(final Collection<String> issues) {
		return new Trace(issues, emptyMap());
	}

	public static Trace trace(final Collection<String> issues, final Map<String, Trace> fields) {
		return new Trace(issues, fields);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final List<String> issues;
	private final Map<String, Trace> fields;


	private Trace(final Collection<String> issues, final Map<String, Trace> fields) {

		if ( issues == null || issues.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null issues");
		}

		if ( fields == null
				|| fields.keySet().stream().anyMatch(Objects::isNull)
				|| fields.values().stream().anyMatch(Objects::isNull)
		) {
			throw new NullPointerException("null fields");
		}

		this.issues=issues.stream()
				.filter(issue -> !issue.isEmpty())
				.collect(toList());

		this.fields=fields.entrySet().stream()
				.filter(field -> !field.getValue().isEmpty())
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}


	public boolean isEmpty() {
		return issues.isEmpty() && fields.isEmpty();
	}


	public List<String> getIssues() {
		return unmodifiableList(issues);
	}

	public Map<String, Trace> getFields() {
		return unmodifiableMap(fields);
	}


	@Override public String toString() {
		return String.format("{\n\tissue: %s\n\tfields: %s\n}", issues, fields);
	}

}
