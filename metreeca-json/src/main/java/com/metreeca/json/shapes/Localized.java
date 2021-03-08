/*
 * Copyright © 2013-2021 Metreeca srl
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

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Lang.lang;


/**
 * Language localization constraint.
 *
 * <p>States that the focus set contains a unique localized string for each a language tag.</p>
 */
public final class Localized extends Shape {

	// always include a lang() shape as a hook for Shape.localize(tags)

	public static Shape localized(final String... tags) {

		if ( tags == null || Arrays.stream(tags).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null tags");
		}

		if ( Arrays.stream(tags).anyMatch(String::isEmpty) ) {
			throw new IllegalArgumentException("empty tags");
		}

		return and(new Localized(), lang(tags));
	}

	public static Shape localized(final Collection<String> tags) {

		if ( tags == null || tags.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null tags");
		}

		if ( tags.stream().anyMatch(String::isEmpty) ) {
			throw new IllegalArgumentException("empty tags");
		}

		return and(new Localized(), lang(tags));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Localized() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return object instanceof Localized;
	}

	@Override public int hashCode() {
		return Localized.class.hashCode();
	}

	@Override public String toString() {
		return "localized()";
	}

}
