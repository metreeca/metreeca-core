/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.form.codecs;

import com.metreeca.form.Shape;
import com.metreeca.form.things.ValuesTest;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Meta.alias;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Values.inverse;
import static com.metreeca.form.things.Values.iri;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.singletonMap;


final class JSONCodecTest {

	private Map<IRI, String> aliases(final Shape shape) {
		return new JSONCodec() {}.aliases(shape);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testGuessAliasFromIRI() {

		assertThat(aliases(field(RDF.VALUE)))
				.as("direct")
				.isEqualTo(singletonMap(RDF.VALUE, "value"));

		assertThat(aliases(field(inverse(RDF.VALUE))))
				.as("inverse")
				.isEqualTo(singletonMap(inverse(RDF.VALUE), "valueOf"));

	}

	@Test void testRetrieveUserDefinedAlias() {
		assertThat(aliases(field(RDF.VALUE, alias("alias"))))
				.as("user-defined")
				.isEqualTo(singletonMap(RDF.VALUE, "alias"));
	}

	@Test void testPreferUserDefinedAliases() {
		assertThat(aliases(and(field(RDF.VALUE, alias("alias")), field(RDF.VALUE))))
				.as("user-defined")
				.isEqualTo(map(entry(RDF.VALUE, "alias")));
	}


	@Test void testRetrieveAliasFromNestedShapes() {

		assertThat(aliases(and(field(RDF.VALUE, alias("alias")))))
				.as("group")
				.isEqualTo(map(entry(RDF.VALUE, "alias")));

		assertThat(aliases(field(RDF.VALUE, and(alias("alias")))))
				.as("conjunction")
				.isEqualTo(map(entry(RDF.VALUE, "alias")));

	}

	@Test void testMergeDuplicateFields() {

		// nesting required to prevent and() from collapsing duplicates
		assertThat(aliases(and(field(RDF.VALUE), and(field(RDF.VALUE)))))
				.as("system-guessed")
				.isEqualTo(map(entry(RDF.VALUE, "value")));

		// nesting required to prevent and() from collapsing duplicates
		assertThat(aliases(and(field(RDF.VALUE, alias("alias")), and(field(RDF.VALUE, alias("alias"))))))
				.as("user-defined")
				.isEqualTo(map(entry(RDF.VALUE, "alias")));

	}


	@Test void testHandleMultipleAliases() {

		assertThat(aliases(field(RDF.VALUE, and(alias("one"), alias("two")))))
				.as("clashing")
				.isEqualTo(map(entry(RDF.VALUE, "value")));

		assertThat(aliases(field(RDF.VALUE, and(alias("one"), alias("one")))))
				.as("repeated")
				.isEqualTo(map(entry(RDF.VALUE, "one")));

	}

	@Test void testMergeAliases() {
		assertThat(aliases(and(field(RDF.TYPE), field(RDF.VALUE))))
				.as("merged")
				.isEqualTo(map(entry(RDF.TYPE, "type"), entry(RDF.VALUE, "value")));
	}

	@Test void testIgnoreClashingAliases() {

		assertThat(aliases(and(field(RDF.VALUE), field(iri("urn:example:value")))))
				.as("different fields")
				.isEmpty();

		// fall back to system-guess alias

		assertThat(aliases(and(field(RDF.VALUE, alias("one")), field(RDF.VALUE, alias("two")))))
				.as("same field")
				.isEqualTo(map(entry(RDF.VALUE, "value")));

	}

	@Test void testIgnoreReservedAliases() {

		assertThat(aliases(field(iri(ValuesTest.Base, "this"))))
				.as("ignore reserved system-guessed aliases")
				.isEmpty();

		assertThat(aliases(field(RDF.VALUE, alias("this"))))
				.as("ignore reserved user-defined aliases")
				.isEqualTo(singletonMap(RDF.VALUE, "value"));

	}

}
