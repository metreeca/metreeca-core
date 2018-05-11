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

package com.metreeca.spec.shapes;

import com.metreeca.jeep.rdf.Values;
import com.metreeca.spec.Shape;

import org.eclipse.rdf4j.model.Value;


/**
 * Default value annotation.
 *
 * <p>Provides the default for the expected values of enclosing shape.</p>
 */
public final class Default implements Shape {

	public static Default dflt(final Value value) {
		return new Default(value);
	}


	private final Value value;


	public Default(final Value value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		this.value=value;
	}


	public Value getValue() {
		return value;
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Default
				&& value.equals(((Default)object).value);
	}

	@Override public int hashCode() {
		return value.hashCode();
	}

	@Override public String toString() {
		return "dflt("+Values.format(value)+")";
	}

}
