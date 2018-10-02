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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;


public final class Maps {

	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static <K, V> Map<K, V> map() {
		return emptyMap();
	}

	@SafeVarargs public static <K, V> Map<K, V> map(final Map.Entry<K, V>... entries) {

		if ( entries == null ) {
			throw new NullPointerException("null entries");
		}

		return map(asList(entries));
	}

	public static <K, V> Map<K, V> map(final Collection<Map.Entry<K, V>> entries) {

		if ( entries == null ) {
			throw new NullPointerException("null entries");
		}

		if ( entries.contains(null) ) {
			throw new NullPointerException("null entry");
		}

		final Map<K, V> map=new LinkedHashMap<>();

		for (final Map.Entry<K, V> entry : entries) {
			map.put(entry.getKey(), entry.getValue());
		}

		return unmodifiableMap(map);
	}


	public static <K, V> Map.Entry<K, V> entry(final K key, final V value) {
		return new AbstractMap.SimpleImmutableEntry<>(key, value);
	}


	//// Operators /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static <K, V> Map<K, V> union(final Map<K, V> x, final Map<K, V> y) {

		if ( x == null ) {
			throw new NullPointerException("null x");
		}

		final Map<K, V> map=new LinkedHashMap<>();

		map.putAll(x);
		map.putAll(y);

		return unmodifiableMap(map);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Maps() {}

}
