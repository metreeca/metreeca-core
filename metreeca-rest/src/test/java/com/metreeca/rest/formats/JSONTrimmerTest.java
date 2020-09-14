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

import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.util.Map;

import static com.metreeca.json.JSONAssert.assertThat;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static java.util.Collections.emptyMap;
import static javax.json.Json.createObjectBuilder;

final class JSONTrimmerTest {

	private JsonValue trim(final Shape shape, final JsonObjectBuilder object) {
		return trim(shape, emptyMap(), object);
	}

	private JsonValue trim(final Shape shape, final Map<String, String> keywords, final JsonObjectBuilder object) {
		return new JSONTrimmer(keywords).trim(iri(), shape, object.build());
	}


	@Test void testPreserveKeywords() {
		assertThat(trim(field(RDF.VALUE), createObjectBuilder()

				.add("@id", "http://example.com/"))

		).isEqualTo(createObjectBuilder()

				.add("@id", "http://example.com/")

		);
	}

	@Test void testPreserveAliasedKeywords() {
		assertThat(trim(field(RDF.VALUE), map(entry("@id", "id")), createObjectBuilder()

				.add("id", "http://example.com/"))

		).isEqualTo(createObjectBuilder()

				.add("id", "http://example.com/")

		);
	}

	@Test void testPruneField() {
		assertThat(trim(field(RDF.VALUE), createObjectBuilder()

				.add("value", 1)
				.add("other", 2))

		).isEqualTo(createObjectBuilder()

				.add("value", 1)

		);
	}

	@Test void testTraverseAnd() {
		assertThat(trim(and(field(RDF.FIRST), field(RDF.REST)), createObjectBuilder()

				.add("first", 1)
				.add("rest", 2)
				.add("other", 3))

		).isEqualTo(createObjectBuilder()

				.add("first", 1)
				.add("rest", 2)

		);
	}

	@Test void testTraverseField() {
		assertThat(trim(field(RDF.FIRST, field(RDF.REST)), createObjectBuilder()

				.add("first", createObjectBuilder()
						.add("rest", 2)
						.add("other", 4)
				)
				.add("other", 3))).isEqualTo(createObjectBuilder()

				.add("first", createObjectBuilder()
						.add("rest", 2)
				)

		);
	}

	@Test void testTraverseOr() {
		assertThat(trim(or(field(RDF.FIRST), field(RDF.REST)), createObjectBuilder()

				.add("first", 1)
				.add("rest", 2)
				.add("other", 3))

		).isEqualTo(createObjectBuilder()

				.add("first", 1)
				.add("rest", 2)

		);
	}

	@Test void testTraverseWhen() {
		assertThat(trim(when(clazz(RDF.NIL), field(RDF.FIRST), field(RDF.REST)), createObjectBuilder()

				.add("first", 1)
				.add("rest", 2)
				.add("other", 3))

		).isEqualTo(createObjectBuilder()

				.add("first", 1)
				.add("rest", 2)

		);
	}

}