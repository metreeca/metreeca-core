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

package com.metreeca.json;

import com.metreeca.json.shapes.Field;

import java.util.*;

import static com.metreeca.json.Values.format;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;


/**
 * Ordering criterion.
 */
public final class Order {

	public static Order increasing(final Field... path) {
		return new Order(false, asList(path));
	}

	public static Order increasing(final List<Field> path) {
		return new Order(false, path);
	}


	public static Order decreasing(final Field... path) {
		return new Order(true, asList(path));
	}

	public static Order decreasing(final List<Field> path) {
		return new Order(true, path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final boolean inverse;
	private final List<Field> path;


	private Order(final boolean inverse, final List<Field> path) {

		if ( path == null || path.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null path or path step");
		}

		this.inverse=inverse;
		this.path=new ArrayList<>(path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean inverse() {
		return inverse;
	}

	public List<Field> path() {
		return unmodifiableList(path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Order
				&& path.equals(((Order)object).path)
				&& inverse == ((Order)object).inverse;
	}

	@Override public int hashCode() {
		return path.hashCode()^Boolean.hashCode(inverse);
	}

	@Override public String toString() {

		final StringBuilder builder=new StringBuilder(20*path.size());

		for (final Field step : path) {

			if ( builder.length() > 0 ) { builder.append('/'); }

			if ( !step.direct() ) { builder.append('^'); }

			builder.append(format(step.name()));
		}

		return builder.insert(0, inverse ? "-" : "+").toString();
	}

}
