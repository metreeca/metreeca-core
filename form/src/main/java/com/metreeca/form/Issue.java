/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.form;

import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.Locale;

import static com.metreeca.form.Shape.pass;
import static com.metreeca.form.things.Sets.set;


/**
 * Shape validation issue.
 */
public final class Issue {

	/**
	 * Validation severity levels.
	 */
	public enum Level {
		Info, Warning, Error
	}


	public static Issue issue(final Level level, final String message, final Shape shape) {
		return new Issue(level, message, shape, set());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Level level; // the severity level
	private final String message; // a human readable description
	private final Shape shape; // the shape this issue node was generated from


	private Issue(final Level level, final String message, final Shape shape, final Collection<Value> values) {

		if ( level == null ) {
			throw new NullPointerException("null level");
		}

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		this.level=level;
		this.message=message;
		this.shape=shape;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Level getLevel() {
		return level;
	}

	public String getMessage() {
		return message;
	}

	public Shape getShape() {
		return shape;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Tests if the severity of this issue reaches an expected level.
	 *
	 * @param limit the expected severity level
	 *
	 * @return {@code true} if the severity level of this issue is greater tha or equal to the severity {@code limit}
	 *
	 * @throws NullPointerException if {@code limit} is null
	 */
	public boolean assess(final Level limit) {

		if ( limit == null ) {
			throw new NullPointerException("null limit");
		}

		return level.compareTo(limit) >= 0;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Issue
				&& level == ((Issue)object).level
				&& message.equals(((Issue)object).message)
				&& shape.equals(((Issue)object).shape);
	}

	@Override public int hashCode() {
		return level.hashCode()^message.hashCode()^shape.hashCode();
	}

	@Override public String toString() {
		return level.toString().toLowerCase(Locale.ROOT)+" : "+message+(pass(shape)? "" : " : "+shape);
	}

}
