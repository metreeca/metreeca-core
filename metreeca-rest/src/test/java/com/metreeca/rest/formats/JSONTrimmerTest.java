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

package com.metreeca.rest.formats;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.JSONAssert.assertThat;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static javax.json.Json.createObjectBuilder;

final class JSONTrimmerTest {

	@Test void testPruneField() {
		assertThat(new JSONTrimmer().trim(

				createObjectBuilder()

						.add("value", 1)
						.add("other", 2)

						.build(),

				field(RDF.VALUE)

		)).isEqualTo(createObjectBuilder()

				.add("value", 1)

		);
	}

	@Test void testTraverseAnd() {
		assertThat(new JSONTrimmer().trim(

				createObjectBuilder()

						.add("first", 1)
						.add("rest", 2)
						.add("other", 3)

						.build(),

				and(field(RDF.FIRST), field(RDF.REST))

		)).isEqualTo(createObjectBuilder()

				.add("first", 1)
				.add("rest", 2)

		);
	}

	@Test void testTraverseField() {
		assertThat(new JSONTrimmer().trim(

				createObjectBuilder()

						.add("first", createObjectBuilder()
								.add("rest", 2)
								.add("other", 4)
						)
						.add("other", 3)

						.build(),

				field(RDF.FIRST, field(RDF.REST))

		)).isEqualTo(createObjectBuilder()

				.add("first", createObjectBuilder()
						.add("rest", 2)
				)

		);
	}

	@Test void testTraverseOr() {
		assertThat(new JSONTrimmer().trim(

				createObjectBuilder()

						.add("first", 1)
						.add("rest", 2)
						.add("other", 3)

						.build(),

				or(field(RDF.FIRST), field(RDF.REST))

		)).isEqualTo(createObjectBuilder()

				.add("first", 1)
				.add("rest", 2)

		);
	}

	@Test void testTraverseWhen() {
		assertThat(new JSONTrimmer().trim(

				createObjectBuilder()

						.add("first", 1)
						.add("rest", 2)
						.add("other", 3)

						.build(),

				when(clazz(RDF.NIL), field(RDF.FIRST), field(RDF.REST)) // !!! actually test

		)).isEqualTo(createObjectBuilder()

				.add("first", 1)
				.add("rest", 2)

		);
	}

}