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

package com.metreeca.json;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.metreeca.json.Frame.*;
import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;

final class FrameTest {

	private static final IRI w=iri("http://example.com/w");
	private static final IRI x=iri("http://example.com/x");
	private static final IRI y=iri("http://example.com/y");
	private static final IRI z=iri("http://example.com/z");


	@Nested final class Assembling {

		@Test void testHandleDirectAndInverseFields() {

			final Frame frame=frame(x).set(RDF.VALUE).value(y).set(inverse(RDF.VALUE)).value(z);

			assertThat(frame.get(RDF.VALUE).value().orElse(RDF.NIL)).isEqualTo(y);
			assertThat(frame.get(inverse(RDF.VALUE)).value().orElse(RDF.NIL)).isEqualTo(z);
		}


		@Test void testReportLiteralSubjectsForDirectFields() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> {
						frame(literal(1)).set(RDF.VALUE).value(x);
					});
		}

		@Test void testReportLiteralObjectsForInverseFields() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> {
						frame(x).set(inverse(RDF.VALUE)).value(literal(1));
					});
		}

	}

	@Nested final class Importing {

		@Test void testImportDirectStatements() {
			assertThat(frame(x, singletonList(

					statement(x, RDF.VALUE, y)

			)).model()).isIsomorphicTo(

					statement(x, RDF.VALUE, y)

			);
		}

		@Test void testImportInverseStatements() {
			assertThat(frame(x, singletonList(

					statement(y, RDF.VALUE, x)

			)).model()).isIsomorphicTo(

					statement(y, RDF.VALUE, x)

			);
		}

		@Test void testIgnoreUnconnectedStatements() {
			assertThat(frame(x, asList(

					statement(x, RDF.VALUE, y),
					statement(w, RDF.VALUE, z)

			)).model()).isIsomorphicTo(

					statement(x, RDF.VALUE, y)

			);
		}

		@Test void testImportTransitiveStatements() {
			assertThat(frame(x, asList(

					statement(x, RDF.FIRST, y),
					statement(y, RDF.REST, z)

			)).model()).isIsomorphicTo(

					statement(x, RDF.FIRST, y),
					statement(y, RDF.REST, z)

			);
		}

		@Test void testImportRecursiveModels() {
			assertThat(frame(x, asList(

					statement(x, RDF.VALUE, y),
					statement(y, RDF.VALUE, x)

					)).get(seq(RDF.VALUE, RDF.VALUE, RDF.VALUE))
							.value()
							.orElseGet(() -> fail("missing transitive value"))

			).isEqualTo(y);
		}
	}

	@Nested final class Exporting {

		@Test void testExportDirectStatements() {
			assertThat(frame(x).set(RDF.VALUE).value(y)

					.model()

			).isIsomorphicTo(

					statement(x, RDF.VALUE, y)

			);
		}

		@Test void testExportInverseStatements() {
			assertThat(frame(x).set(inverse(RDF.VALUE)).value(y)

					.model()

			).isIsomorphicTo(

					statement(y, RDF.VALUE, x)

			);
		}

		@Test void testExportTransitiveStatements() {
			assertThat(frame(x)

					.set(RDF.FIRST).frame(frame(y)
							.set(RDF.REST).value(z)
					)

					.model()

			).isIsomorphicTo(

					statement(x, RDF.FIRST, y),
					statement(y, RDF.REST, z)

			);
		}

	}

	@Nested final class Traversing {

		private Set<Value> get(final BiFunction<? super Value, ? super Collection<Statement>, Stream<Value>> path) {
			return frame(x, asList(

					statement(x, RDF.FIRST, y),
					statement(y, RDF.FIRST, literal(1)),
					statement(y, RDF.REST, literal(2)),

					statement(x, RDF.REST, z),
					statement(z, RDF.FIRST, literal(3)),
					statement(z, RDF.REST, literal(4))

			)).get(path).values().collect(toCollection(LinkedHashSet::new));
		}


		@Test void testSeq() {
			assertThat(get(seq(RDF.FIRST, RDF.REST))).containsExactly(literal(2));
		}

		@Test void testAlt() {
			assertThat(get(alt(RDF.FIRST, RDF.REST))).containsExactly(y, z);
		}

	}

}