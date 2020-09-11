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

import org.eclipse.rdf4j.model.IRI;

import java.util.*;

import static com.metreeca.json.Values.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;


/**
 * Ordering criterion.
 */
public final class Order {

	public static Order increasing(final IRI... path) {
		return new Order(false, asList(path));
	}

	public static Order increasing(final List<IRI> path) {
		return new Order(false, path);
	}


	public static Order decreasing(final IRI... path) {
		return new Order(true, asList(path));
	}

	public static Order decreasing(final List<IRI> path) {
		return new Order(true, path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final boolean inverse;
	private final List<IRI> path;


	private Order(final boolean inverse, final List<IRI> path) {

		if ( path == null || path.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null path or path step");
		}

		this.inverse=inverse;
		this.path=new ArrayList<>(path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean inverse() {
		return inverse;
	}

	public List<IRI> path() {
		return unmodifiableList(path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Order
				&& path.equals(((Order)object).path)
				&& inverse == ((Order)object).inverse;
	}

	@Override public int hashCode() {
		return path.hashCode()^Boolean.hashCode(inverse);
	}

	@Override public String toString() {

		final StringBuilder builder=new StringBuilder(20*path.size());

		for (final IRI step : path) {

			if ( builder.length() > 0 ) {
				builder.append('/');
			}

			builder.append(format(step));
		}

		return builder.insert(0, inverse ? "-" : "+").toString();
	}

}
