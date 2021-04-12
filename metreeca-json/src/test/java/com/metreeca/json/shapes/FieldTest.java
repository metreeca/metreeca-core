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

package com.metreeca.json.shapes;

import com.metreeca.json.Values;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Field.labels;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import static java.util.Collections.singletonMap;


final class FieldTest {

	@Nested final class Optimization {

		@Test void testPruneDeadPaths() {
			assertThat(field(RDF.VALUE, or())).isEqualTo(and());
		}

	}

	@Nested class Labels {

		@Test void testInspectAnd() {
			assertThat(labels(and(

					field(RDF.FIRST),
					field(RDF.REST)

			))).containsKeys("first", "rest");
		}

		@Test void testInspectOr() {
			assertThat(labels(or(

					field(RDF.FIRST),
					field(RDF.REST)

			))).containsKeys("first", "rest");
		}

		@Test void testInspectWhen() {
			assertThat(labels(when(

					datatype(RDF.NIL),
					field(RDF.FIRST),
					field(RDF.REST)

			))).containsKeys("first", "rest");
		}

		@Test void testInspectOtherShapes() {
			assertThat(labels(and())).isEmpty();
		}


		@Test void testGuessLabelFromIRI() {

			assertThat(labels(field(RDF.VALUE)))
					.as("direct")
					.containsKey("value");

			assertThat(labels(field(Values.inverse(RDF.VALUE))))
					.as("inverse")
					.containsKey("valueOf");

		}

		@Test void testRetrieveUserDefinedLabel() {
			assertThat(labels(field("alias", RDF.VALUE)))
					.as("user-defined")
					.containsKey("alias");
		}

		@Test void testPreferUserDefinedFields() {
			assertThat(labels(and(field("alias", RDF.VALUE), field(RDF.VALUE))))
					.as("user-defined")
					.containsKey("alias");
		}


		@Test void testReportConflictingFields() {
			assertThatIllegalArgumentException().isThrownBy(() -> labels(and(
					field("alias", RDF.FIRST),
					field("alias", RDF.REST)
			)));
		}

		@Test void testReportConflictingProperties() {
			assertThatIllegalArgumentException().isThrownBy(() -> labels(and(
					field(RDF.VALUE),
					field(iri("urn:example#value"))
			)));
		}

		@Test void testReportReservedLabels() {

			assertThatIllegalArgumentException().isThrownBy(() ->
					labels(field(iri("http://exampe.com/", "@id")))
			);

			assertThatIllegalArgumentException().isThrownBy(() ->
					labels(field("@id", RDF.VALUE))
			);

			assertThatIllegalArgumentException().isThrownBy(() ->
					labels(field("id", RDF.VALUE), singletonMap("@id", "id"))
			);

		}

	}

}
