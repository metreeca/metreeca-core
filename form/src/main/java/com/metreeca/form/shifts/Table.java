/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.form.shifts;

import com.metreeca.form.Shift;
import com.metreeca.form.shapes.Trait;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.metreeca.form.things.Maps.map;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;


public final class Table implements Shift {

	@SafeVarargs public static Table table(final Map.Entry<Trait, Shift>... fields) {
		return table(map(fields));
	}

	public static Table table(final Map<Trait, Shift> fields) {
		return new Table(fields);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<Trait, Shift> fields;


	private Table(final Map<Trait, Shift> fields) {

		if ( fields == null ) {
			throw new NullPointerException("null fields");
		}

		if ( fields.containsKey(null) ) {
			throw new NullPointerException("null trait");
		}

		if ( fields.containsValue(null) ) {
			throw new NullPointerException("null shift");
		}


		this.fields=new LinkedHashMap<>(fields);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Map<Trait, Shift> getFields() {
		return unmodifiableMap(fields);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Table
				&& fields.equals(((Table)object).fields);
	}

	@Override public int hashCode() {
		return fields.hashCode();
	}

	@Override public String toString() {
		return fields.entrySet().stream().map(
				field -> field.getKey().getStep()+" -> "+field.getValue()
		).collect(joining(",\n", "{\n", "\n}"));
	}

}
