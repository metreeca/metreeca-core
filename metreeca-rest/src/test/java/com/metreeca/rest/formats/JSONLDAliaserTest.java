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
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Values.inverse;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.relate;
import static com.metreeca.json.shapes.Meta.alias;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static com.metreeca.rest.formats.JSONLDAliaser.aliases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.util.Collections.singletonMap;

final class JSONLDAliaserTest {

	@Test void testInspectAnd() {
		assertThat(aliases(and(

				field(RDF.FIRST),
				field(RDF.REST)

		))).containsKeys("first", "rest");
	}

	@Test void testInspectOr() {
		assertThat(aliases(or(

				field(RDF.FIRST),
				field(RDF.REST)

		))).containsKeys("first", "rest");
	}

	@Test void testInspectWhen() {
		assertThat(aliases(when(

				relate(),
				field(RDF.FIRST),
				field(RDF.REST)

		))).containsKeys("first", "rest");
	}

	@Test void testInspectOtherShapes() {
		assertThat(aliases(and())).isEmpty();
	}


	@Test void testGuessAliasFromIRI() {

		assertThat(aliases(field(RDF.VALUE)))
				.as("direct")
				.containsKey("value");

		assertThat(aliases(field(inverse(RDF.VALUE), and())))
				.as("inverse")
				.containsKey("valueOf"); // !!! inverse?

	}

	@Test void testRetrieveUserDefinedAlias() {
		assertThat(aliases(field(RDF.VALUE, alias("alias"))))
				.as("user-defined")
				.containsKey("alias");
	}

	@Test void testPreferUserDefinedfields() {
		assertThat(aliases(and(field(RDF.VALUE, alias("alias")), field(RDF.VALUE))))
				.as("user-defined")
				.containsKey("alias");
	}


	@Test void testReportConflictingAliases() {
		assertThatThrownBy(() -> {
			aliases(field(RDF.VALUE, and(
					alias("one"),
					alias("two")
			)));
		}).isInstanceOf(IllegalArgumentException.class);
	}

	@Test void testMergeDuplicateAliases() {
		assertThat(aliases(field(RDF.VALUE, and(alias("one"), alias("one")))))
				.containsKeys("one");
	}


	@Test void testReportConflictingFields() {
		assertThatThrownBy(() -> {
			aliases(and(
					field(RDF.VALUE, alias("one")),
					field(RDF.VALUE, alias("two"))
			));
		}).isInstanceOf(IllegalArgumentException.class);
	}

	@Test void testMergeDuplicateFields() {
		assertThat(aliases(and(
				field(RDF.VALUE, alias("one")),
				field(RDF.VALUE, alias("one"))
		))).containsKeys("one");
	}


	@Test void testReportConflictingProperties() {
		assertThatThrownBy(() -> {
			aliases(and(field(RDF.VALUE), field(iri("urn:example#value"), and())));
		})
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test void testReportReservedAliases() {

		assertThatThrownBy(() -> {
			aliases(field(iri(ValuesTest.Base, "@id"), and()));
		})
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> {
			aliases(field(RDF.VALUE, alias("@id")));
		})
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> aliases(field(RDF.VALUE, alias("id")), singletonMap("@id", "id")))
				.isInstanceOf(IllegalArgumentException.class);

	}

}
