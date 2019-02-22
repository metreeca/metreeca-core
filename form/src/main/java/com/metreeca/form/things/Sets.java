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

package com.metreeca.form.things;

import java.util.*;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;


/**
 * Set utilities.
 */
public final class Sets {

	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static <V> Set<V> set() {
		return emptySet();
	}

	public static <V> Set<V> set(final V item) {
		return singleton(item);
	}

	@SafeVarargs public static <V> Set<V> set(final V... items) {
		return items == null ? emptySet() : unmodifiableSet(Arrays
				.stream(items)
				.collect(toSet())
		);
	}

	public static <V> Set<V> set(final Iterable<? extends V> items) {
		return items == null ? emptySet() : unmodifiableSet(StreamSupport
				.stream(items.spliterator(), false)
				.collect(toSet())
		);
	}


	//// Operators /////////////////////////////////////////////////////////////////////////////////////////////////////

	@SafeVarargs public static <V> Set<V> union(final Iterable<? extends V>... collections) {
		return collections == null ? emptySet() : unmodifiableSet(Arrays
				.stream(collections)
				.filter(Objects::nonNull)
				.flatMap(collection -> StreamSupport.stream(collection.spliterator(), false))
				.collect(toSet())
		);
	}

	public static <V> Set<V> intersection(final Collection<? extends V> x, final Collection<? extends V> y) {
		return x == null || y == null ? emptySet() : unmodifiableSet(x
				.stream()
				.filter(y::contains)
				.collect(toSet())
		);
	}

	public static <V> Set<V> complement(final Collection<? extends V> x, final Collection<? extends V> y) {
		return x == null ? emptySet() : unmodifiableSet(x
				.stream()
				.filter(o -> y != null && !y.contains(o))
				.collect(toSet())
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Sets() {}

}
