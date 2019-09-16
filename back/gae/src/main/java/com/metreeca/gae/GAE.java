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

package com.metreeca.gae;


import com.metreeca.tree.Shape;

import com.google.appengine.api.datastore.*;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.tree.shapes.Clazz.clazz;

import static com.google.appengine.api.datastore.KeyFactory.createKey;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;


/**
 * Standard field names and data types.
 */
public final class GAE {

	private static final Pattern DotPattern=Pattern.compile("\\.");


	//// System Properties /////////////////////////////////////////////////////////////////////////////////////////////

	public static final String id="id";
	public static final String type="type";
	public static final String label="label";

	public static final String contains="contains";

	public static final String terms="terms";
	public static final String term="term";

	public static final String stats="stats";

	public static final String count="count";
	public static final String min="min";
	public static final String max="max";


	//// System Datatypes (Sorting Order) //////////////////////////////////////////////////////////////////////////////

	public static final String Integral="Integral";
	public static final String Date="Date";
	public static final String Boolean="Boolean";
	public static final String String="String";
	public static final String Floating="Floating";
	public static final String Key="Key";
	public static final String Entity="Entity";


	public static String type(final Object value) {
		return isIntegral(value) ? Integral
				: isDate(value) ? Date
				: isBoolean(value) ? Boolean
				: isString(value) ? String
				: isFloating(value) ? Floating
				: isKey(value) ? Key
				: isEntity(value) ? Entity
				: null;
	}


	/**
	 * Tests if an object is an integral numeric value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is an integral numeric value; {@code false}, otherwise
	 */
	public static boolean isIntegral(final Object o) {
		return o instanceof Long || o instanceof Integer || o instanceof Short;
	}

	/**
	 * Tests if an object is a date value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is a date value; {@code false}, otherwise
	 */
	public static boolean isDate(final Object o) {
		return o instanceof Date;
	}

	/**
	 * Tests if an object is a boolean value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is a boolean value; {@code false}, otherwise
	 */
	public static boolean isBoolean(final Object o) {
		return o instanceof Boolean;
	}

	/**
	 * Tests if an object is a string value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is a string value; {@code false}, otherwise
	 */
	public static boolean isString(final Object o) {
		return o instanceof String;
	}

	/**
	 * Tests if an object is a floating-point numeric value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is a floating-point numeric value; {@code false}, otherwise
	 */
	public static boolean isFloating(final Object o) {
		return o instanceof Double || o instanceof Float;
	}

	/**
	 * Tests if an object is a daastore key value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is a datastore key value; {@code false}, otherwise
	 */
	public static boolean isKey(final Object o) {
		return o instanceof Key;
	}

	/**
	 * Tests if an object is an entity value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is an entity value; {@code false}, otherwise
	 */
	public static boolean isEntity(final Object o) {
		return o instanceof PropertyContainer;
	}


	//// Key Generation ////////////////////////////////////////////////////////////////////////////////////////////////

	public static Key key(final String path) { // !!! tbd

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return key(path, "");
	}

	public static Key key(final String path, final Shape shape) { // !!! tbd

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return key(path, clazz(shape).orElse(""));

	}

	/**
	 * Creates a datastore key for a resource.
	 *
	 * @param path the path of the resource; falls back to {@code "/"} if empty
	 * @param type the type of the resource; falls back to {@value #Entity} if empty
	 *
	 * @return a datastore key for the resource identified by {@code path} and {@code type}
	 *
	 * @throws NullPointerException     if either {@code path} or {@code type} is null
	 * @throws IllegalArgumentException if {@code path} is not empty and doesn't include a leading slash
	 */
	public static Key key(final String path, final String type) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( !path.isEmpty() && !path.startsWith("/") ) {
			throw new IllegalArgumentException("illegal path {"+path+"}");
		}

		if ( type == null ) {
			throw new NullPointerException("null type");
		}

		Key ancestor=null;

		for (int slash=0; slash >= 0; slash=path.indexOf('/', slash+1)) {
			if ( slash > 0 && slash+1 < path.length() ) { // ignore leading/trailing slashes

				ancestor=createKey(ancestor, Entity, path.substring(0, slash+1));

			}
		}

		return createKey(ancestor, type.isEmpty()? Entity : type, path.isEmpty()? "/" : path);
	}


	//// Entity Utilities //////////////////////////////////////////////////////////////////////////////////////////////

	private static Key key(final PropertyContainer container) {

		if ( container == null ) {
			throw new NullPointerException("null container");
		}

		return container instanceof Entity ? ((Entity)container).getKey()
				: container instanceof EmbeddedEntity ? ((EmbeddedEntity)container).getKey()
				: null;
	}

	public static String kind(final PropertyContainer container) {

		if ( container == null ) {
			throw new NullPointerException("null container");
		}

		final Key key=key(container);

		return key != null ? key.getKind() : null;
	}


	public static Object get(final PropertyContainer container, final String path) {
		return get(container, DotPattern.split(path));
	}

	public static Object get(final PropertyContainer container, final String... path) {
		return get(container, asList(path));
	}

	public static Object get(final PropertyContainer container, final Iterable<String> path) {
		return get(container, null, path);
	}


	public static <T> T get(final PropertyContainer container, final T fallback, final String path) {
		return get(container, fallback, DotPattern.split(path));
	}

	public static <T> T get(final PropertyContainer container, final T fallback, final String... path) {
		return get(container, fallback, asList(path));
	}

	@SuppressWarnings("unchecked") public static <T> T get(final PropertyContainer container, final T fallback, final Iterable<String> path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		Object value=container;

		for (final String step : path) {

			if ( step == null ) {
				throw new NullPointerException("null step");
			}

			value=(value instanceof PropertyContainer) ? ((PropertyContainer)value).getProperty(step)

					: (value instanceof Collection) ? ((Collection<?>)value).stream()
					.map(v -> v instanceof PropertyContainer ? ((PropertyContainer)v).getProperty(step) : null)
					.filter(Objects::nonNull)
					.flatMap(v -> v instanceof Collection ? ((Collection<?>)v).stream() : Stream.of(v))
					.collect(toList())

					: null;
		}

		return value != null ? (T)value : fallback;
	}


	public static EmbeddedEntity embed(final Entity entity) {

		if ( entity == null ) {
			throw new NullPointerException("null entity");
		}

		final EmbeddedEntity resource=new EmbeddedEntity();

		resource.setKey(entity.getKey());
		resource.setPropertiesFrom(entity);

		return resource;
	}


	//// Sorting ///////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Compares two object for ordering.
	 *
	 * <p>Comparison is consistent with Google Datastore <a href="https://cloud.google.com/appengine/docs/standard/java/datastore/entity-property-reference#properties_and_value_types">sorting
	 * rules</a>, with the following deterministic ordering for mixed value types:</p>
	 *
	 * <ul>
	 *     <li>null values;</li>
	 *     <li>integral values;</li>
	 *     <li>date values;</li>
	 *     <li>boolean values;</li>
	 *     <li>string values;</li>
	 *     <li>floating-point values;</li>
	 *     <li>datastore key values;</li>
	 *     <li>property container values (label/id/key order);</li>
	 *     <li>other values (system-dependent order).</li>
	 * </ul>
	 *
	 * @param x the first object to be compared.
	 * @param y the second object to be compared.
	 *
	 * @return a negative integer, zero, or a positive integer as the {@code x} is less than, equal to, or greater than
	 * {@code y}
	 */
	public static int compare(final Object x, final Object y) {
		return x == null ? y == null ? 0 : -1 : y == null ? 1

				: isIntegral(x) ? isIntegral(y) ? Integral(x, y) : -1 : isIntegral(y) ? 1

				: isDate(x) ? isDate(y) ? Date(x, y) : -1 : isDate(y) ? 1

				: isBoolean(x) ? isBoolean(y) ? Boolean(x, y) : -1 : isBoolean(y) ? 1

				: isString(x) ? isString(y) ? String(x, y) : -1 : isString(y) ? 1

				: isFloating(x) ? isFloating(y) ? Floating(x, y) : -1 : isFloating(y) ? 1

				: isKey(x) ? isKey(y) ? Key(x, y) : -1 : isKey(y) ? 1

				: isEntity(x) ? isEntity(y) ? Entity(x, y) : -1 : isEntity(y) ? 1

				: Other(x, y);
	}


	private static int Integral(final Object x, final Object y) {
		return Long.compare(((Number)x).longValue(), ((Number)y).longValue());
	}

	private static int Date(final Object x, final Object y) {
		return ((Date)x).compareTo((Date)y);
	}

	private static int Boolean(final Object x, final Object y) {
		return ((Boolean)x).compareTo((Boolean)y);
	}

	private static int String(final Object x, final Object y) {
		return ((String)x).compareTo((String)y);
	}

	private static int Floating(final Object x, final Object y) {
		return Double.compare(((Number)x).doubleValue(), ((Number)y).doubleValue());
	}

	private static int Key(final Object x, final Object y) {
		return ((Key)x).compareTo((Key)y);
	}

	private static int Entity(final Object x, final Object y) {

		final Function<Object, Object> order=v -> Stream.<Function<Object, Object>>of(

				o -> ((PropertyContainer)o).getProperty(label),
				o -> ((PropertyContainer)o).getProperty(id),

				o -> o instanceof Entity ? ((Entity)o).getKey() : null,
				o -> o instanceof EmbeddedEntity ? ((EmbeddedEntity)o).getKey() : null

		)

				.map(f -> f.apply(v))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);

		return compare(order.apply(x), order.apply(y));
	}

	private static int Other(final Object x, final Object y) {
		return Integer.compare(System.identityHashCode(x), System.identityHashCode(y));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private GAE() {} // utility

}
