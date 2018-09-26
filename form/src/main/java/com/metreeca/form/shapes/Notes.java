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

import com.metreeca.form.Issue;
import com.metreeca.form.Shape;


/**
 * Textual annotation.
 *
 * <p>Provides a human-readable textual description of the enclosing shape, also referenced by validation
 * {@link Issue issues} to report the results of custom hard-coded inspections.</p>
 */
public final class Notes implements Shape {

	public static Notes notes(final String text) {
		return new Notes(text);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String text;


	private Notes(final String text) {

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
		return this == object || object instanceof Notes
				&& text.equals(((Notes)object).text);
	}

	@Override public int hashCode() {
		return text.hashCode();
	}

	@Override public String toString() {
		return "notes("+text+")";
	}

}
