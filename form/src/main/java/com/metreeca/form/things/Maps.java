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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;


/**
 * Map utilities.
 */
public final class Maps {

	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static <K, V> Map<K, V> map() {
		return emptyMap();
	}

	@SafeVarargs public static <K, V> Map<K, V> map(final Map.Entry<? extends K, ? extends V>... entries) {
		return entries == null ? emptyMap() : collect(Arrays.stream(entries));
	}

	public static <K, V> Map<K, V> map(final Iterable<Map.Entry<? extends K, ? extends V>> entries) {
		return entries == null ? emptyMap() : collect(StreamSupport.stream(entries.spliterator(), false));
	}


	public static <K, V> Map.Entry<K, V> entry(final K key, final V value) {
		return new AbstractMap.SimpleImmutableEntry<>(key, value);
	}


	//// Operators /////////////////////////////////////////////////////////////////////////////////////////////////////

	@SafeVarargs public static <K, V> Map<K, V> union(final Map<? extends K, ? extends V>... maps) {
		return maps == null ? emptyMap() : collect(Arrays
				.stream(maps)
				.filter(Objects::nonNull)
				.map(Map::entrySet)
				.flatMap(Collection::stream)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// ;(jdk) Collectors.toMap doesn't support null entry values…
	// https://stackoverflow.com/questions/24630963/java-8-nullpointerexception-in-collectors-tomap

	private static <K, V> Map<K, V> collect(final Stream<Map.Entry<? extends K, ? extends V>> stream) {
		return unmodifiableMap(stream.filter(Objects::nonNull).collect(HashMap::new, (map, entry) -> {

			final K key=entry.getKey();
			final V value=entry.getValue();

			if ( !map.containsKey(key) ) {
				map.put(key, value);
			} else if ( !Objects.equals(value, map.put(key, value)) ) {
				throw new IllegalStateException(String.format("value collision {%s} -> {%s}", key, value));
			}

		}, HashMap::putAll));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Maps() {}

}
