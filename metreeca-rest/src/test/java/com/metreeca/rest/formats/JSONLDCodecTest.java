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

import com.metreeca.json.ValuesTest;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.json.JsonException;

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
import static com.metreeca.rest.formats.JSONLDCodec.fields;
import static com.metreeca.rest.formats.JSONLDCodec.maxCount;
import static com.metreeca.rest.formats.JSONLDCodec.minCount;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class JSONLDCodecTest {

	@Nested final class MinCountProbe {

		@Test void testInspectMinCount() {
			assertThat(minCount(minCount(10))).contains(10);
		}

		@Test void testInspectAnd() {
			assertThat(minCount(and(minCount(10), minCount(100)))).contains(100);
		}

		@Test void testInspectDisjunction() {
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
					.isInstanceOf(JsonException.class);
		}

		@Test void testReportReservedAliases() {

			assertThatThrownBy(() -> fields(field(iri(ValuesTest.Base, "@id"), and())))
					.isInstanceOf(JsonException.class);

			assertThatThrownBy(() -> fields(field(RDF.VALUE, alias("@id"))))
					.isInstanceOf(JsonException.class);

			assertThatThrownBy(() -> fields(field(RDF.VALUE, alias("id")), singletonMap("@id", "id")))
					.isInstanceOf(JsonException.class);

		}

	}

	@Test void test() {
		System.out.println(fields(field(RDF.VALUE, alias("alias"))));
	}

}