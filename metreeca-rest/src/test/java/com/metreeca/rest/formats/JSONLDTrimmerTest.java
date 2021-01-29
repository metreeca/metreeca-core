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

package com.metreeca.rest.formats;

import com.metreeca.json.Shape;
import com.metreeca.rest.Xtream;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.util.Map;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Lang.lang;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static com.metreeca.rest.JSONAssert.assertThat;
import static java.util.Collections.emptyMap;
import static javax.json.Json.createObjectBuilder;

final class JSONLDTrimmerTest {

	private JsonValue trim(final Shape shape, final JsonObjectBuilder object) {
		return trim(shape, emptyMap(), object);
	}

	private JsonValue trim(final Shape shape, final Map<String, String> keywords, final JsonObjectBuilder object) {
		return new JSONLDTrimmer(iri(), shape, keywords).trim(object.build());
	}


	@Test void testPreserveKeywords() {
		assertThat(trim(field(RDF.VALUE), createObjectBuilder()

				.add("@id", "http://example.com/"))

		).isEqualTo(createObjectBuilder()

				.add("@id", "http://example.com/")

		);
	}

	@Test void testPreserveAliasedKeywords() {
		assertThat(trim(field(RDF.VALUE), Xtream.map(Xtream.entry("@id", "id")), createObjectBuilder()

				.add("id", "http://example.com/"))

		).isEqualTo(createObjectBuilder()

				.add("id", "http://example.com/")

		);
	}


	@Test void testPruneField() {
		assertThat(trim(field(RDF.VALUE), createObjectBuilder()

				.add("value", 1)
				.add("other", 2)

		)).isEqualTo(createObjectBuilder()

				.add("value", 1)

		);
	}

	@Test void testPruneLanguageContainers() {
		assertThat(trim(field(RDF.VALUE, lang("en")), createObjectBuilder()

				.add("value", createObjectBuilder()
						.add("en", "one")
						.add("it", "uno")
				)

		)).isEqualTo(createObjectBuilder()

				.add("value", createObjectBuilder()
						.add("en", "one")
				)

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