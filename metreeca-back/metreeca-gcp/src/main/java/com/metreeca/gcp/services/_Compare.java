/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.gcp.services;

import com.google.cloud.datastore.*;

import static com.google.cloud.datastore.ValueType.*;

final class _Compare {


	//// Sorting ///////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Compares values.
	 *
	 * <p>Comparison is consistent with Google Datastore
	 * <a href="https://cloud.google.com/datastore/docs/concepts/entities#value_type_ordering">sorting
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
	static int compare(final Value<?> x, final Value<?> y) {

		if ( x == null ) {
			throw new NullPointerException("null x");
		}

		if ( y == null ) {
			throw new NullPointerException("null y");
		}

		final ValueType xtype=x.getType();
		final ValueType ytype=y.getType();

		return xtype == NULL ? ytype == NULL ? 0 : -1 : ytype == NULL ? 1

				: xtype == LONG ? ytype == LONG ? compare((LongValue)x, (LongValue)y) : -1 : ytype == LONG ? 1

				: xtype == TIMESTAMP ? ytype == TIMESTAMP ? compare((TimestampValue)x, (TimestampValue)y) : -1 :
				ytype == TIMESTAMP ? 1

						: xtype == BOOLEAN ? ytype == BOOLEAN ? compare((BooleanValue)x, (BooleanValue)y) : -1 :
						ytype == BOOLEAN ? 1

								// !!! blobs

								: xtype == STRING ? ytype == STRING ? compare((StringValue)x, (StringValue)y) : -1 :
								ytype == STRING ? 1

								: xtype == DOUBLE ? ytype == DOUBLE ? compare((DoubleValue)x, (DoubleValue)y) : -1 :
										ytype == DOUBLE ? 1

								// !!! points

								: xtype == KEY ? ytype == KEY ? compare((KeyValue)x, (KeyValue)y) : -1 :
												ytype == KEY ? 1

								: xtype == ENTITY ? ytype == ENTITY ? compare((EntityValue)x, (EntityValue)y) : -1 :
														ytype == ENTITY ? 1

								: 0;
	}


	private static int compare(final LongValue x, final LongValue y) {
		return Long.compare(x.get(), y.get());
	}

	private static int compare(final TimestampValue x, final TimestampValue y) {
		return x.get().compareTo(y.get());
	}

	private static int compare(final BooleanValue x, final BooleanValue y) {
		return Boolean.compare(x.get(), y.get());
	}

	private static int compare(final StringValue x, final StringValue y) {
		return x.get().compareTo(y.get());
	}

	private static int compare(final DoubleValue x, final DoubleValue y) {
		return Double.compare(x.get(), y.get());
	}

	private static int compare(final KeyValue x, final KeyValue y) {
		return compare(x.get(), y.get());
	}

	private static int compare(final EntityValue x, final EntityValue y) {
		return compare(x.get().getKey(), y.get().getKey());
	}

	private static int compare(final IncompleteKey x, final IncompleteKey y) {
		return (x == null ? "" : x.toString()).compareTo(y == null ? "" : y.toString()); // !!! review
	}

}
