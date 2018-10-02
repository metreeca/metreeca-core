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

package com.metreeca.form.things;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;


public final class Lists {

	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static <V> List<V> list() {
		return emptyList();
	}

	public static <V> List<V> list(final V item) {
		return singletonList(item);
	}

	@SafeVarargs public static <V> List<V> list(final V... items) {

		if ( items == null ) {
			throw new NullPointerException("null items");
		}

		return list(asList(items)); // copy items to prevent write-through from array
	}

	public static <V> List<V> list(final Collection<V> items) {

		if ( items == null ) {
			throw new NullPointerException("null items");
		}

		return unmodifiableList(new ArrayList<>(items));
	}


	//// Operators /////////////////////////////////////////////////////////////////////////////////////////////////////

	@SafeVarargs public static <V> List<V> concat(final Collection<V>... collections) {

		if ( collections == null ) {
			throw new NullPointerException("null collections");
		}

		return unmodifiableList(Stream.of(collections)
				.map(Objects::requireNonNull)
				.flatMap(Collection::stream)
				.collect(toList()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Lists() {}

}
