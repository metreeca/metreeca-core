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

import java.util.Date;
import java.util.function.Function;

import static com.metreeca.tree.shapes.Clazz.clazz;

import static com.google.appengine.api.datastore.KeyFactory.createKey;


/**
 * Standard field names and data types.
 */
public final class GAE {

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


	//// System Datatypes //////////////////////////////////////////////////////////////////////////////////////////////

	public static final String Entity="Entity";
	public static final String Boolean="Boolean";
	public static final String Integral="Integral";
	public static final String Floating="Floating";
	public static final String String="String";
	public static final String Date="Date";


	public static String type(final Object value) {
		return Entity(value)? Entity
				: Boolean(value)? Boolean
				: Integral(value)? Integral
				: Floating(value)? Floating
				: String(value)? String
				: Date(value)? Date
				: null;
	}


	/**
	 * Tests if an object is an entity value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is an entity value; {@code false}, otherwise
	 */
	public static boolean Entity(final Object o) {
		return o instanceof PropertyContainer;
	}

	/**
	 * Tests if an object is a boolean value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is a boolean value; {@code false}, otherwise
	 */
	public static boolean Boolean(final Object o) {
		return o instanceof Boolean;
	}

	/**
	 * Tests if an object is an integral numeric value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is an integral numeric value; {@code false}, otherwise
	 */
	public static boolean Integral(final Object o) {
		return o instanceof Long || o instanceof Integer || o instanceof Short;
	}

	/**
	 * Tests if an object is a floating-point numeric value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is a floating-point numeric value; {@code false}, otherwise
	 */
	public static boolean Floating(final Object o) {
		return o instanceof Double || o instanceof Float;
	}

	/**
	 * Tests if an object is a string value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is a string value; {@code false}, otherwise
	 */
	public static boolean String(final Object o) {
		return o instanceof String;
	}

	/**
	 * Tests if an object is a date value.
	 *
	 * @param o the object to be tested
	 *
	 * @return {@code true}, if {@code o} is a date value; {@code false}, otherwise
	 */
	public static boolean Date(final Object o) {
		return o instanceof Date;
	}


	//// Key Generation ////////////////////////////////////////////////////////////////////////////////////////////////

	public static Key root(final String path) { // !!! tbd

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return createKey("*", path.substring(0, path.lastIndexOf('/')+1));
	}

	public static Key key(final String path, final Shape shape) { // !!! tbd

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return clazz(shape)

				.map(kind -> key(path, kind))

				.orElseGet(() -> createKey("*", path));

	}

	public static Key key(final String path, final String kind) { // !!! tbd

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( kind == null ) {
			throw new NullPointerException("null kind");
		}

		return createKey(root(path), kind, path);
	}


	//// Sorting ///////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Compares two object for ordering.
	 *
	 * <p>Comparison is consistent with Google Datastore <a href="https://cloud.google.com/appengine/docs/standard/java/datastore/entity-property-reference#properties_and_value_types">sorting
	 * rules</a>, with the following deterministi c ordering for mixed value types:</p>
	 *
	 * <ul>
	 *     <li>null values;</li>
	 *     <li>integral values;</li>
	 *     <li>date values;</li>
	 *     <li>boolean values;</li>
	 *     <li>string values;</li>
	 *     <li>floating-point values;</li>
	 *     <li>floating-point values;</li>
	 *     <li>property container values (key order);</li>
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

				: Integral(x) ? Integral(y) ? Integral(x, y) : -1 : Integral(y) ? 1

				: Date(x) ? Date(y) ? Date(x, y) : -1 : Date(y) ? 1

				: Boolean(x) ? Boolean(y) ? Boolean(x, y) : -1 : Boolean(y) ? 1

				: String(x) ? String(y) ? String(x, y) : -1 : String(y) ? 1

				: Floating(x) ? Floating(y) ? Double(x, y) : -1 : Floating(y) ? 1

				: Entity(x) ? Entity(y) ? Entity(x, y) : -1 : Entity(y) ? 1

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

	private static int Double(final Object x, final Object y) {
		return Double.compare(((Number)x).doubleValue(), ((Number)y).doubleValue());
	}

	private static int Entity(final Object x, final Object y) {

		final Function<Object, Key> key=o -> o instanceof Entity ? ((Entity)o).getKey()
				: o instanceof EmbeddedEntity ? ((EmbeddedEntity)o).getKey()
				: null;

		final Key kx=key.apply(x);
		final Key ky=key.apply(y);

		return kx == null ? ky == null ? 0 : -1
				: ky == null ? 1
				: kx.compareTo(ky);
	}

	private static int Other(final Object x, final Object y) {
		return Integer.compare(System.identityHashCode(x), System.identityHashCode(y));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private GAE() {} // utility

}
