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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shifts.Seq.seq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

final class FrameTest {

	private static final IRI w=iri("http://example.com/w");
	private static final IRI x=iri("http://example.com/x");
	private static final IRI y=iri("http://example.com/y");
	private static final IRI z=iri("http://example.com/z");

	@Nested final class Assembling {

		@Test void testHandleDirectAndInverseFields() {

			final Frame frame=frame(x)
					.add(RDF.VALUE, y)
					.add(inverse(RDF.VALUE), z);

			assertThat(frame.get(RDF.VALUE)).containsExactly(y);
			assertThat(frame.get(inverse(RDF.VALUE))).containsExactly(z);
		}

		@Test void testHandleNestedFrames() {

			final Frame frame=frame(w)
					.add(RDF.VALUE, x)
					.add(RDF.VALUE, frame(y)
							.add(RDF.VALUE, w)
					)
					.add(RDF.VALUE, z);

			assertThat(frame.model()).isIsomorphicTo(
					statement(w, RDF.VALUE, x),
					statement(w, RDF.VALUE, y),
					statement(y, RDF.VALUE, w),
					statement(w, RDF.VALUE, z)
			);
		}

		@Test void testReportLiteralObjectsForInverseFields() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> {
						frame(x).add(inverse(RDF.VALUE), literal(1));
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
			assertThat(

					frame(x, asList(

							statement(x, RDF.VALUE, y),
							statement(y, RDF.VALUE, x)

					)).get(seq(RDF.VALUE, RDF.VALUE, RDF.VALUE))

			).containsExactly(y);
		}
	}

	@Nested final class Exporting {

		@Test void testExportDirectStatements() {
			assertThat(frame(x).add(RDF.VALUE, y)

					.model()

			).isIsomorphicTo(

					statement(x, RDF.VALUE, y)

			);
		}

		@Test void testExportInverseStatements() {
			assertThat(frame(x).add(inverse(RDF.VALUE), y)

					.model()

			).isIsomorphicTo(

					statement(y, RDF.VALUE, x)

			);
		}

		@Test void testExportTransitiveStatements() {
			assertThat(frame(x)

					.add(RDF.FIRST, frame(y)
							.add(RDF.REST, z)
					)

					.model()

			).isIsomorphicTo(

					statement(x, RDF.FIRST, y),
					statement(y, RDF.REST, z)

			);
		}

	}

}