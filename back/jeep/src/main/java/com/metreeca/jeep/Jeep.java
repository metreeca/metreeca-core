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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;


/**
 * Static utility methods
 */
public final class Jeep {

	private Jeep() {} // a utility class


	//// Collections ///////////////////////////////////////////////////////////////////////////////////////////////////

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


	//// Maps //////////////////////////////////////////////////////////////////////////////////////////////////////////

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
		return new SimpleImmutableEntry<>(key, value);
	}


	//// Set Operators /////////////////////////////////////////////////////////////////////////////////////////////////

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


	//// Collection Operators //////////////////////////////////////////////////////////////////////////////////////////

	@SafeVarargs public static <V> Set<V> union(final Collection<V>... collections) {
		return collections == null ? set() : Stream.of(collections)
				.filter(collection -> collection != null)
				.flatMap(Collection::stream)
				.distinct()
				.collect(toCollection(LinkedHashSet::new));
	}

	@SafeVarargs public static <V> List<V> concat(final Collection<V>... collections) {
		return collections == null ? list() : Stream.of(collections)
				.filter(collection -> collection != null)
				.flatMap(Collection::stream)
				.collect(toList());
	}


	//// Map Operators /////////////////////////////////////////////////////////////////////////////////////////////////

	public static <K, V> Map<K, V> union(final Map<K, V> x, final Map<K, V> y) {

		if ( x == null ) {
			throw new NullPointerException("null x");
		}

		final Map<K, V> map=new LinkedHashMap<>();

		map.putAll(x);
		map.putAll(y);

		return map;
	}


	//// Comparables ///////////////////////////////////////////////////////////////////////////////////////////////////

	public static <T extends Comparable<T>> boolean eq(final T x, final T y) { return compare(x, y) == 0; }

	public static <T extends Comparable<T>> boolean ne(final T x, final T y) { return compare(x, y) != 0; }

	public static <T extends Comparable<T>> boolean lt(final T x, final T y) { return compare(x, y) < 0; }

	public static <T extends Comparable<T>> boolean lte(final T x, final T y) { return compare(x, y) <= 0; }

	public static <T extends Comparable<T>> boolean gt(final T x, final T y) { return compare(x, y) > 0; }

	public static <T extends Comparable<T>> boolean gte(final T x, final T y) { return compare(x, y) >= 0; }


	public static <T extends Comparable<T>> T max(final T x, final T y) { return compare(x, y) >= 0 ? x : y; }

	public static <T extends Comparable<T>> T min(final T x, final T y) { return compare(x, y) <= 0 ? x : y; }


	public static <T extends Comparable<T>> int compare(final T x, final T y) {
		return x == null ? y == null ? 0 : -1 : y == null ? 1 : x.compareTo(y);
	}


	//// Combinators ///////////////////////////////////////////////////////////////////////////////////////////////////

	public static <T> BinaryOperator<T> nullable(final BinaryOperator<T> operator) { // !!! review

		if ( operator == null ) {
			throw new NullPointerException("null operator");
		}

		return (x, y) -> x == null ? y : y == null ? x : operator.apply(x, y);
	}


	//// Exception Handling ////////////////////////////////////////////////////////////////////////////////////////////

	public static <R> R guard(final Supplier<R> supplier, final Function<Throwable, R> handler) {

		if ( supplier == null ) {
			throw new NullPointerException("null supplier");
		}

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		try {
			return supplier.get();
		} catch ( final RuntimeException e ) {
			return handler.apply(e);
		}

	}


	//// Caching ///////////////////////////////////////////////////////////////////////////////////////////////////////

	public static <V> Supplier<V> cache(final Supplier<V> supplier) {

		if ( supplier == null ) {
			throw new NullPointerException("null supplier");
		}

		return new Supplier<V>() {

			private V value;

			@Override public V get() {
				return value != null ? value : (value=supplier.get());
			}

		};
	}

	public static <V, R> Function<V, R> cache(final Function<V, R> function) {

		if ( function == null ) {
			throw new NullPointerException("null function");
		}

		return new Function<V, R>() {

			private final Map<V, R> cache=new HashMap<V, R>();

			@Override public R apply(final V v) {
				return cache.computeIfAbsent(v, function);
			}

		};
	}


	//// Strings ///////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String lower(final String string) {
		return string == null ? null : string.toLowerCase(Locale.ROOT);
	}

	public static String upper(final String string) {
		return string == null ? null : string.toUpperCase(Locale.ROOT);
	}

	public static String title(final String string) {
		if ( string == null || string.isEmpty() ) { return string; } else {

			final StringBuilder builder=new StringBuilder(string.length());

			boolean trailing=false;

			for (int n=string.length(), i=0; i < n; ++i) {

				final char c=string.charAt(i);

				builder.append(trailing ? c : Character.toUpperCase(c));

				trailing=Character.isAlphabetic(c);
			}

			return builder.toString();
		}
	}

	public static String capital(final String string) {
		return string == null || string.isEmpty() ? string
				: Character.toUpperCase(string.charAt(0))+string.substring(1);
	}


	public static String normalize(final String string) {
		if ( string == null || string.isEmpty() ) { return string; } else {

			final StringBuilder builder=new StringBuilder(string.length());

			boolean spacing=false;

			for (int n=string.length(), i=0; i < n; ++i) {

				final char c=string.charAt(i);

				if ( !Character.isWhitespace(c) ) {

					if ( spacing ) {
						builder.append(' ');
					}

					builder.append(c);

					spacing=false;

				} else if ( builder.length() > 0 ) {

					spacing=true;

				}

			}

			return builder.toString();
		}
	}


	public static String indent(final Object object) {
		return indent(object, false);
	}

	public static String indent(final Object object, final boolean trailing) {
		return object == null ? null : object.toString().replaceAll((trailing ? "(\n)" : "(^|\n)")+"([^\n])", "$1\t$2");
	}

}
