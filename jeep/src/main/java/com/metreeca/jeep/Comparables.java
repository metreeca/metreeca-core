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

public final class Comparables {

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Comparables() {}

}
