/*
 * Copyright © 2013-2020 Metreeca srl
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

package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;


/**
 * Language value constraint.
 *
 * <p>States that each value in the focus set is a localized string, with a language tag possibly restricted to a
 * given set of target values.</p>
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

		return new Lang(tags);
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
