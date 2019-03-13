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

package com.metreeca.form.things;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;


/**
 * List utilities.
 */
public final class Lists {

	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static <V> List<V> list() {
		return emptyList();
	}

	public static <V> List<V> list(final V item) {
		return singletonList(item);
	}

	@SafeVarargs public static <V> List<V> list(final V... items) {
		return items == null ? emptyList() : unmodifiableList(Arrays
				.stream(items)
				.collect(toList())
		);
	}

	public static <V> List<V> list(final Iterable<? extends V> items) {
		return items == null ? emptyList() : unmodifiableList(StreamSupport
				.stream(items.spliterator(), false)
				.collect(toList())
		);
	}


	//// Operators /////////////////////////////////////////////////////////////////////////////////////////////////////

	@SafeVarargs public static <V> List<V> concat(final Iterable<? extends V>... collections) {
		return collections == null ? emptyList() : unmodifiableList(Arrays
				.stream(collections)
				.filter(Objects::nonNull)
				.flatMap(collection -> StreamSupport.stream(collection.spliterator(), false))
				.collect(toList())
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Lists() {}

}
