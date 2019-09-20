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


/**
 * Standard field names and data types.
 */
public final class GAE {

	//// System Classes ////////////////////////////////////////////////////////////////////////////////////////////////

	public static final String Resource="Resource";


	//// System Properties /////////////////////////////////////////////////////////////////////////////////////////////

	public static final String id="id";
	public static final String value="value";
	public static final String type="type";
	public static final String language="language";

	public static final String label="label";
	public static final String contains="contains";

	public static final String terms="terms";
	public static final String stats="stats";

	public static final String count="count";
	public static final String min="min";
	public static final String max="max";


	//// Sorting ///////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Compares values.
	 *
	 * <p>Comparison is consistent with Google Datastore <a href="https://cloud.google.com/datastore/docs/concepts/entities#value_type_ordering">sorting
	 * rules</a>, with the following deterministic ordering for mixed value types:</p>
	 *
	 * <ul>
	 *     <li>null values;</li>
	 *     <li>integer values;</li>
	 *     <li>timestamp values;</li>
	 *     <li>boolean values;</li>
	 *     <li>blob values;</li>
	 *     <li>string values;</li>
	 *     <li>double values;</li>
	 *     <li>point values;</li>
	 *     <li>key values;</li>
	 *     <li>entity values;</li>
	 *     <li>other values (system-dependent order).</li>
	 * </ul>
	 *
	 * @param x the first object to be compared.
	 * @param y the second object to be compared.
	 *
	 * @return a negative integer, zero, or a positive integer as the {@code x} is less than, equal to, or greater than
	 * {@code y}
	 */
	//public static int compare(final Value x, final Value y) {
	//	return x == null ? y == null ? 0 : -1 : y == null ? 1
	//
	//			: isIntegral(x) ? isIntegral(y) ? Integral(x, y) : -1 : isIntegral(y) ? 1
	//
	//			: isDate(x) ? isDate(y) ? Date(x, y) : -1 : isDate(y) ? 1
	//
	//			: isBoolean(x) ? isBoolean(y) ? Boolean(x, y) : -1 : isBoolean(y) ? 1
	//
	//          // !!! blobs
	//
	//			: isString(x) ? isString(y) ? String(x, y) : -1 : isString(y) ? 1
	//
	//			: isFloating(x) ? isFloating(y) ? Floating(x, y) : -1 : isFloating(y) ? 1
	//
	//          // !!! points
	//
	//			: isKey(x) ? isKey(y) ? Key(x, y) : -1 : isKey(y) ? 1
	//
	//			: isEntity(x) ? isEntity(y) ? Entity(x, y) : -1 : isEntity(y) ? 1
	//
	//			: Other(x, y);
	//}
	//
	//
	//private static int Integral(final Object x, final Object y) {
	//	return Long.compare(((Number)x).longValue(), ((Number)y).longValue());
	//}
	//
	//private static int Date(final Object x, final Object y) {
	//	return ((Date)x).compareTo((Date)y);
	//}
	//
	//private static int Boolean(final Object x, final Object y) {
	//	return ((Boolean)x).compareTo((Boolean)y);
	//}
	//
	//private static int String(final Object x, final Object y) {
	//	return ((String)x).compareTo((String)y);
	//}
	//
	//private static int Floating(final Object x, final Object y) {
	//	return Double.compare(((Number)x).doubleValue(), ((Number)y).doubleValue());
	//}
	//
	//private static int Key(final Object x, final Object y) {
	//	return ((Key)x).compareTo((Key)y);
	//}
	//
	//private static int Entity(final Object x, final Object y) {
	//
	//	final Comparator<Entity> comparator=comparing(container -> // ensure total ordering
	//			Optional.ofNullable(key(container)).orElseGet(() -> createKey(Entity, identityHashCode(container)))
	//	);
	//
	//	return comparator.compare((Entity)x, (Entity)y);
	//}
	//
	//private static int Other(final Object x, final Object y) {
	//	return Integer.compare(identityHashCode(x), identityHashCode(y));
	//}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private GAE() {} // utility

}
