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

package com.metreeca.spec.shifts;

import com.metreeca.spec.Shift;


public final class Count implements Shift {

	public static Count count(final Shift shift) {
		return new Count(shift);
	}


	private final Shift shift;


	public Count(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		this.shift=shift;
	}


	public Shift getShift() {
		return shift;
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Count
				&& shift.equals(((Count)object).shift);
	}

	@Override public int hashCode() {
		return shift.hashCode();
	}

	@Override public String toString() {
		return "count("+shift+")";
	}

}
