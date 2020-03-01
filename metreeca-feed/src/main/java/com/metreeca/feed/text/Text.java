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

package com.metreeca.feed.text;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;


/**
 * Focus switching task.
 *
 * <p>Generates a stream of parametrized units.</p>
 */
public final class Text<V> implements Function<V, Stream<String>> {

	private final TextTemplate template;

	private final Map<String, Function<V, Stream<String>>> parameters=new LinkedHashMap<>();


	public Text(final String template) {

		if ( template == null ) {
			throw new NullPointerException("null template");
		}

		this.template=new TextTemplate(template);
	}


	public Text<V> parameter(final String name, final Object... values) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		return parameter(name, unit -> Stream.of(values)
				.filter(Objects::nonNull)
				.map(Object::toString)
		);
	}

	public Text<V> parameter(final String name, final Function<V, Stream<String>> expression) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( expression == null ) {
			throw new NullPointerException("null expression");
		}

		parameters.put(name, expression);

		return this;
	}


	@Override public Stream<String> apply(final V item) {

		// compute the cartesian product of parameter values computed on value

		Stream<Function<String, String>> resolvers=Stream.of(key -> null);

		for (final Map.Entry<String, Function<V, Stream<String>>> entry : parameters.entrySet()) {

			final String name=entry.getKey();
			final Function<V, Stream<String>> expression=entry.getValue();

			resolvers=resolvers.parallel().flatMap(resolver -> requireNonNull(expression.apply(item))
					.filter(Objects::nonNull)
					.map(value -> key ->
							key.equals(name) ? value : resolver.apply(key)
					));

		}

		// fill template

		return resolvers.parallel().map(template::fill);

	}

}