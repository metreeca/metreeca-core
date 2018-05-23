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

package com.metreeca.spec.shapes;

import com.metreeca.spec.Shape;
import com.metreeca.spec.Shift;


public final class Virtual implements Shape {

	public static Virtual virtual(final Trait trait, final Shift shift) {
		return new Virtual(trait, shift);
	}


	private final Trait trait;
	private final Shift shift;


	public Virtual(final Trait trait, final Shift shift) {

		if ( trait == null ) {
			throw new NullPointerException("null trait");
		}

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		this.trait=trait;
		this.shift=shift;
	}


	public Trait getTrait() {
		return trait;
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
		return this == object || object instanceof Virtual
				&& trait.equals(((Virtual)object).trait)
				&& shift.equals(((Virtual)object).shift);
	}

	@Override public int hashCode() {
		return trait.hashCode()^shift.hashCode();
	}

	@Override public String toString() {
		return "virtual("+trait+", "+shift+")";
	}

}
