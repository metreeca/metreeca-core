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

package com.metreeca.rest._work;

import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import javax.json.JsonException;

import static com.metreeca.json.Values.inverse;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rest._work._ValueParser.path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class _ValueParserTest {

	private final Shape shape=and(
			field(RDF.FIRST, field(RDF.REST, and())),
			field(inverse(RDF.FIRST), field(RDF.REST, and()))
	);


	@Test void testEmptyPath() {
		assertThat(path(shape, "")).isEmpty();
	}

	@Test void testDirectAlias() {
		assertThat(path(shape, "first")).containsExactly(RDF.FIRST);
	}

	@Test void testInverseAlias() { // !!! firstOf
		assertThat(path(shape, "firstOf")).containsExactly(inverse(RDF.FIRST));
	}

	@Test void testMultipleSteps() {
		assertThat(path(shape, "first.rest")).containsExactly(RDF.FIRST, RDF.REST);
		assertThat(path(shape, "firstOf.rest")).containsExactly(inverse(RDF.FIRST), RDF.REST);
	}


	@Test void testRejectUnknownPathSteps() {
		assertThatThrownBy(() -> path(shape, "first/unknown")).isInstanceOf(JsonException.class);
	}

	@Test void testRejectMalformedPaths() {
		assertThatThrownBy(() -> path(shape, "---")).isInstanceOf(JsonException.class);
	}

}
