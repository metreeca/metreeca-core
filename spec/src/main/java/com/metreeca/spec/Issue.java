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

package com.metreeca.spec;

import org.eclipse.rdf4j.model.Value;

import java.util.*;

import static com.metreeca.jeep.Jeep.set;
import static com.metreeca.jeep.rdf.Values.format;

import static java.util.Collections.unmodifiableSet;


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


	public static Issue issue(final Level level, final String message, final Shape shape, final Value... values) {
		return new Issue(level, message, shape, set(values));
	}

	public static Issue issue(final Level level, final String message, final Shape shape, final Collection<Value> values) {
		return new Issue(level, message, shape, values);
	}


	private final Level level; // the severity level
	private final String message; // a human readable description
	private final Shape shape; // the shape this issue node was generated from
	private final Set<Value> values; // the values this issue refers to


	public Issue(final Level level, final String message, final Shape shape, final Collection<Value> values) {

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

		if ( values.contains(null) ) {
			throw new NullPointerException("null value");
		}

		this.level=level;
		this.message=message;
		this.shape=shape;
		this.values=new LinkedHashSet<>(values);
	}


	public Level getLevel() {
		return level;
	}

	public String getMessage() {
		return message;
	}

	public Shape getShape() {
		return shape;
	}

	public Set<Value> getValues() {
		return unmodifiableSet(values);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Issue
				&& level == ((Issue)object).level
				&& message.equals(((Issue)object).message)
				&& shape.equals(((Issue)object).shape)
				&& values.equals(((Issue)object).values);
	}

	@Override public int hashCode() {
		return level.hashCode()^message.hashCode()^shape.hashCode()^values.hashCode();
	}

	@Override public String toString() {
		return level.toString().toLowerCase(Locale.ROOT)
				+" : "+message
				+" : "+(values.size() == 1
				? format(values.iterator().next())
				: "{"+(values.isEmpty() ? "no" : values.size())+" values}")
				+" : "+shape;
	}

}
