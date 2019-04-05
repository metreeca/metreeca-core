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

package com.metreeca.form;

import org.eclipse.rdf4j.model.IRI;

import java.util.ArrayList;
import java.util.List;

import static com.metreeca.form.things.Values.format;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;


/**
 * Ordering criterion.
 */
public final class Order {

	public static Order increasing(final IRI... path) {
		return new Order(asList(path), false);
	}

	public static Order increasing(final List<IRI> path) {
		return new Order(path, false);
	}


	public static Order decreasing(final IRI... path) {
		return new Order(asList(path), true);
	}

	public static Order decreasing(final List<IRI> path) {
		return new Order(path, true);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final List<IRI> path;
	private final boolean inverse;


	private Order(final List<IRI> path, final boolean inverse) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( path.contains(null) ) {
			throw new IllegalArgumentException("illegal path element");
		}

		this.path=new ArrayList<>(path);
		this.inverse=inverse;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public List<IRI> getPath() {
		return unmodifiableList(path);
	}

	public boolean isInverse() {
		return inverse;
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
