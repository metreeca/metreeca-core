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

import com.metreeca.json.ValuesTest;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Values.inverse;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.detail;
import static com.metreeca.json.shapes.Guard.relate;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.Meta.alias;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static com.metreeca.rest.formats._JSONLDCodec.fields;
import static com.metreeca.rest.formats._JSONLDCodec.maxCount;
import static com.metreeca.rest.formats._JSONLDCodec.minCount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.util.Collections.singletonMap;

final class _JSONLDCodecTest {

	@Nested final class MinCountProbe {

		@Test void testInspectMinCount() {
			assertThat(minCount(minCount(10))).contains(10);
		}

		@Test void testInspectAnd() {
			assertThat(minCount(and(minCount(10), minCount(100)))).contains(100);
		}

		@Test void testInspectOr() {
			assertThat(minCount(or(minCount(10), minCount(100)))).contains(10);
		}

		@Test void testOption() {
			assertThat(minCount(when(detail(), minCount(10), minCount(100)))).contains(10);
			assertThat(minCount(when(detail(), minCount(10), and()))).contains(10);
			assertThat(minCount(when(detail(), and(), and()))).isEmpty();
		}

		@Test void testInspectOtherShape() {
			assertThat(minCount(and())).isEmpty();
		}

	}

	@Nested final class MaxCountProbe {

		@Test void testInspectMaxCount() {
			assertThat(maxCount(maxCount(10))).contains(10);
		}

		@Test void testInspectAnd() {
			assertThat(maxCount(and(maxCount(10), maxCount(100)))).contains(10);
		}

		@Test void testInspectOr() {
			assertThat(maxCount(or(maxCount(10), maxCount(100)))).contains(100);
		}

		@Test void testOption() {
			assertThat(maxCount(when(detail(), maxCount(10), maxCount(100)))).contains(100);
			assertThat(maxCount(when(detail(), maxCount(10), and()))).contains(10);
			assertThat(maxCount(when(detail(), and(), and()))).isEmpty();
		}

		@Test void testInspectOtherShape() {
			assertThat(maxCount(and())).isEmpty();
		}

	}

	@Nested final class FieldsProbe {

		@Test void testInspectAnd() {
			assertThat(fields(and(

					field(RDF.FIRST),
					field(RDF.REST)

			))).containsKeys("first", "rest");
		}

		@Test void testInspectOr() {
			assertThat(fields(or(

					field(RDF.FIRST),
					field(RDF.REST)

			))).containsKeys("first", "rest");
		}

		@Test void testInspectWhen() {
			assertThat(fields(when(

					relate(),
					field(RDF.FIRST),
					field(RDF.REST)

			))).containsKeys("first", "rest");
		}

		@Test void testInspectOtherShapes() {
			assertThat(fields(and())).isEmpty();
		}


		@Test void testGuessAliasFromIRI() {

			assertThat(fields(field(RDF.VALUE)))
					.as("direct")
					.containsKey("value");

			assertThat(fields(field(inverse(RDF.VALUE), and())))
					.as("inverse")
					.containsKey("valueOf"); // !!! inverse?

		}

		@Test void testRetrieveUserDefinedAlias() {
			assertThat(fields(field(RDF.VALUE, alias("alias"))))
					.as("user-defined")
					.containsKey("alias");
		}

		@Test void testPreferUserDefinedfields() {
			assertThat(fields(and(field(RDF.VALUE, alias("alias")), field(RDF.VALUE))))
					.as("user-defined")
					.containsKey("alias");
		}


		@Test void testReportConflictingAliases() {
			assertThatThrownBy(() -> fields(field(RDF.VALUE, and(
					alias("one"),
					alias("two")
			)))).isInstanceOf(IllegalArgumentException.class);
		}

		@Test void testMergeDuplicateAliases() {
			assertThat(fields(field(RDF.VALUE, and(alias("one"), alias("one")))))
					.containsKeys("one");
		}


		@Test void testReportConflictingFields() {
			assertThatThrownBy(() -> fields(and(
					field(RDF.VALUE, alias("one")),
					field(RDF.VALUE, alias("two"))
			))).isInstanceOf(IllegalArgumentException.class);
		}

		@Test void testMergeDuplicateFields() {
			assertThat(fields(and(
					field(RDF.VALUE, alias("one")),
					field(RDF.VALUE, alias("one"))
			))).containsKeys("one");
		}


		@Test void testReportConflictingProperties() {
			assertThatThrownBy(() -> fields(and(field(RDF.VALUE), field(iri("urn:example#value"), and()))))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test void testReportReservedAliases() {

			assertThatThrownBy(() -> fields(field(iri(ValuesTest.Base, "@id"), and())))
					.isInstanceOf(IllegalArgumentException.class);

			assertThatThrownBy(() -> fields(field(RDF.VALUE, alias("@id"))))
					.isInstanceOf(IllegalArgumentException.class);

			assertThatThrownBy(() -> fields(field(RDF.VALUE, alias("id")), singletonMap("@id", "id")))
					.isInstanceOf(IllegalArgumentException.class);

		}

	}

}