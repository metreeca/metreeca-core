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

package com.metreeca.rdf.formats;

import com.metreeca.core.Context;
import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import javax.json.JsonException;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rdf.Values.inverse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

final class _ValueParserTest { // !!! remove

	private void exec(final Runnable... tasks) {
		new Context().exec(tasks).clear();
	}

	private final Shape shape=and(
			field(RDF.FIRST, field(RDF.REST)),
			field(inverse(RDF.FIRST), field(RDF.REST))
	);


	@Test void testParsePaths() {
		exec(() -> {

			assertThat(_ValueParser.path("app:/", shape, ""))
					.as("empty")
					.isEmpty();

			assertThat(_ValueParser.path("app:/", shape, "<"+RDF.FIRST+">"))
					.as("direct iri")
					.containsExactly(RDF.FIRST);

			assertThat(_ValueParser.path("app:/", shape, "^<"+RDF.FIRST+">"))
					.as("inverse iri")
					.containsExactly(inverse(RDF.FIRST));

			assertThat(_ValueParser.path("app:/", shape, "<"+RDF.FIRST+">/<"+RDF.REST+">"))
					.as("iri slash path")
					.containsExactly(RDF.FIRST, RDF.REST);

			assertThat(_ValueParser.path("app:/", shape, "first"))
					.as("direct alias")
					.containsExactly(RDF.FIRST);

			assertThat(_ValueParser.path("app:/", shape, "firstOf"))
					.as("inverse alias")
					.containsExactly(inverse(RDF.FIRST));

			assertThat(_ValueParser.path("app:/", shape, "first/rest"))
					.as("alias slash path")
					.containsExactly(RDF.FIRST, RDF.REST);

			assertThat(_ValueParser.path("app:/", shape, "firstOf.rest"))
					.as("alias dot path")
					.containsExactly(inverse(RDF.FIRST), RDF.REST);

		});
	}


	@Test void testRejectUnknownPathSteps() {
		exec(() -> assertThatExceptionOfType(JsonException.class)
				.isThrownBy(() -> _ValueParser.path("app:/", shape, "first/unknown")));
	}

	@Test void testRejectMalformedPaths() {
		exec(() -> assertThatExceptionOfType(JsonException.class)
				.isThrownBy(() -> _ValueParser.path("app:/", shape, "---")));
	}

}
