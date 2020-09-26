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

package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.Value;

import static com.metreeca.json.Values.format;
import static com.metreeca.json.Values.value;


/**
 * Inclusive maximum value constraint.
 *
 * <p>States that each term in the focus set is less than or equal to a given maximum value, according to <a
 * href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules.</p>
 */
public final class MaxInclusive extends Shape {

	public static Shape maxInclusive(final Object limit) {

		if ( limit == null ) {
			throw new NullPointerException("null limit");
		}

		return new MaxInclusive(value(limit));
	}

	public static Shape maxInclusive(final Value limit) {

		if ( limit == null ) {
			throw new NullPointerException("null limit");
		}

		return new MaxInclusive(limit);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Value limit;


	private MaxInclusive(final Value limit) {
		this.limit=limit;
	}


	public Value limit() {
		return limit;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof MaxInclusive
				&& limit.equals(((MaxInclusive)object).limit);
	}

	@Override public int hashCode() {
		return limit.hashCode();
	}

	@Override public String toString() {
		return "maxInclusive("+format(limit)+")";
	}

}
