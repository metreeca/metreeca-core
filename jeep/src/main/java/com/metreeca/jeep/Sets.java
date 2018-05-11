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

package com.metreeca.jeep;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toCollection;


public final class Sets {

	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static <V> Set<V> set() {
		return emptySet();
	}

	public static <V> Set<V> set(final V item) {
		return singleton(item);
	}

	@SafeVarargs public static <V> Set<V> set(final V... items) {

		if ( items == null ) {
			throw new NullPointerException("null items");
		}

		return unmodifiableSet(new LinkedHashSet<>(asList(items)));
	}

	public static <V> Set<V> set(final Collection<V> items) {

		if ( items == null ) {
			throw new NullPointerException("null items");
		}

		return unmodifiableSet(new LinkedHashSet<>(items));
	}


	//// Operators /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static <V> Set<V> union(final Set<V> x, final Set<V> y) {

		if ( x == null ) {
			throw new NullPointerException("null x");
		}

		if ( y == null ) {
			throw new NullPointerException("null y");
		}

		final Set<V> union=new LinkedHashSet<>(x);

		union.addAll(y);

		return union;
	}

	@SafeVarargs public static <V> Set<V> union(final Collection<V>... collections) {
		return collections == null ? set() : Stream.of(collections)
				.filter(collection -> collection != null)
				.flatMap(Collection::stream)
				.distinct()
				.collect(toCollection(LinkedHashSet::new));
	}

	public static <V> Set<V> intersection(final Set<V> x, final Set<V> y) {

		if ( x == null ) {
			throw new NullPointerException("null x");
		}

		if ( y == null ) {
			throw new NullPointerException("null y");
		}

		final Set<V> intersection=new LinkedHashSet<>(x);

		intersection.retainAll(y);

		return intersection;
	}

	public static <V> Set<V> complement(final Set<V> x, final Set<V> y) {

		if ( x == null ) {
			throw new NullPointerException("null x");
		}

		if ( y == null ) {
			throw new NullPointerException("null y");
		}

		final Set<V> complement=new LinkedHashSet<>(x);

		complement.removeAll(y);

		return complement;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Sets() {}

}
