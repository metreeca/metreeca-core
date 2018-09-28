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

package com.metreeca.form.shapes;

import com.metreeca.form.Shape;


/**
 * Placeholder annotation.
 *
 * <p>Provides a human-readable textual placeholder for the expected values of the enclosing shape.</p>
 */
public final class Placeholder implements Shape {

	public static Placeholder placeholder(final String text) {
		return new Placeholder(text);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String text;


	private Placeholder(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		this.text=text;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String getText() {
		return text;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <V> V accept(final Probe<V> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Placeholder
				&& text.equals(((Placeholder)object).text);
	}

	@Override public int hashCode() {
		return text.hashCode();
	}

	@Override public String toString() {
		return "placeholder("+text+")";
	}

}
