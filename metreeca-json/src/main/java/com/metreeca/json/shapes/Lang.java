/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.util.*;

import static com.metreeca.json.shapes.Or.or;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;


/**
 * Language value constraint.
 *
 * <p>States that each term in the focus set is a localized string with a language tag in the given set of target
 * values.</p>
 */
public final class Lang extends Shape {

	public static Shape lang(final String... tags) {

		if ( tags == null || Arrays.stream(tags).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null tags");
		}

		if ( Arrays.stream(tags).anyMatch(String::isEmpty) ) {
			throw new IllegalArgumentException("empty tags");
		}

		return lang(asList(tags));
	}

	public static Shape lang(final Collection<String> tags) {

		if ( tags == null || tags.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null tags");
		}

		if ( tags.stream().anyMatch(String::isEmpty) ) {
			throw new IllegalArgumentException("empty tags");
		}

		return tags.isEmpty() ? or() : new Lang(tags);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Set<String> tags;


	private Lang(final Collection<String> tags) {
		this.tags=new LinkedHashSet<>(tags);
	}


	public Set<String> tags() {
		return unmodifiableSet(tags);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Lang
				&& tags.equals(((Lang)object).tags);
	}

	@Override public int hashCode() {
		return tags.hashCode();
	}

	@Override public String toString() {
		return "lang("+String.join(", ", tags)+")";
	}

}
