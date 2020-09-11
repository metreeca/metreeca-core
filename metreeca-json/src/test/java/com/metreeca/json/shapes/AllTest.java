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

import org.eclipse.rdf4j.model.Value;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.metreeca.json.Values.literal;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;


final class AllTest {

	private static final Value a=literal(1);
	private static final Value b=literal(2);
	private static final Value c=literal(3);
	private static final Value d=literal(4);

	@Test void testInspectExistential() {

		final All all=all(a, b, c);

		assertThat(all(all))
				.contains(all.values());
	}

	@Test void testInspectConjunction() {

		final All x=all(a, b, c);
		final All y=all(b, c, d);

		assertThat(all(and(x, y)))
				.as("all defined")
				.contains(Stream.concat(x.values().stream(), y.values().stream()).collect(toSet()));

		assertThat(all(and(x, and())))
				.as("some defined")
				.contains(x.values());

		assertThat(all(and(and(), and())))
				.as("none defined")
				.isEmpty();

	}

	@Test void testInspectOtherShape() {
		assertThat(all(and()))
				.isEmpty();
	}

}
