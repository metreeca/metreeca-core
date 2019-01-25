/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form.codecs;

import com.metreeca.form.shapes.Field;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.codecs.BaseCodec.aliases;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Meta.alias;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Values.inverse;
import static com.metreeca.form.things.Values.iri;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;


final class BaseCodecTest {

	@Test void testGuessAliasFromIRI() {

		assertThat(singletonMap(RDF.VALUE, "value"))
				.as("direct")
				.isEqualTo(aliases(Field.field(RDF.VALUE)));

		assertThat(singletonMap(inverse(RDF.VALUE), "valueOf"))
				.as("inverse")
				.isEqualTo(aliases(Field.field(inverse(RDF.VALUE))));

	}

	@Test void testRetrieveUserDefinedAlias() {
		assertThat(singletonMap(RDF.VALUE, "alias"))
				.as("user-defined")
				.isEqualTo(aliases(field(RDF.VALUE, alias("alias"))));
	}

	@Test void testPreferUserDefinedAliases() {
		assertThat(map(entry(RDF.VALUE, "alias")))
				.as("user-defined")
				.isEqualTo(aliases(and(field(RDF.VALUE, alias("alias")), Field.field(RDF.VALUE))));
	}


	@Test void testRetrieveAliasFromNestedShapes() {

		assertThat(map(entry(RDF.VALUE, "alias"))).as("group").isEqualTo(aliases(and(field(RDF.VALUE, alias("alias")))));

		assertThat(map(entry(RDF.VALUE, "alias"))).as("conjunction").isEqualTo(aliases(field(RDF.VALUE, and(alias("alias")))));

	}

	@Test void testMergeDuplicateFields() {

		// nesting required to prevent and() from collapsing duplicates
		assertThat(map(entry(RDF.VALUE, "value"))).as("system-guessed").isEqualTo(aliases(and(Field.field(RDF.VALUE), and(Field.field(RDF.VALUE)))));

		// nesting required to prevent and() from collapsing duplicates
		assertThat(map(entry(RDF.VALUE, "alias"))).as("user-defined")
				.isEqualTo(aliases(and(field(RDF.VALUE, alias("alias")), and(field(RDF.VALUE, alias("alias"))))));

	}


	@Test void testHandleMultipleAliases() {

		assertThat(map(entry(RDF.VALUE, "value"))).as("clashing").isEqualTo(aliases(field(RDF.VALUE, and(alias("one"), alias("two")))));

		assertThat(map(entry(RDF.VALUE, "one"))).as("repeated").isEqualTo(aliases(field(RDF.VALUE, and(alias("one"), alias("one")))));

	}

	@Test void testMergeAliases() {
		assertThat(map(entry(RDF.TYPE, "type"), entry(RDF.VALUE, "value")))
				.as("merged")
				.isEqualTo(aliases(and(Field.field(RDF.TYPE), Field.field(RDF.VALUE))));
	}

	@Test void testIgnoreClashingAliases() {

		assertThat(aliases(and(Field.field(RDF.VALUE), Field.field(iri("urn:example:value")))).isEmpty()).as("different fields").isTrue();

		// fall back to system-guess alias
		assertThat(map(entry(RDF.VALUE, "value"))).as("same field")
				.isEqualTo(aliases(and(field(RDF.VALUE, alias("one")), field(RDF.VALUE, alias("two")))));

	}

	@Test void testIgnoreReservedAliases() {

		assertThat(aliases(Field.field(RDF.VALUE), singleton("value")).isEmpty()).as("ignore reserved system-guessed aliases").isTrue();

		assertThat(singletonMap(RDF.VALUE, "value")).as("ignore reserved user-defined aliases").isEqualTo(aliases(field(RDF.VALUE, alias("reserved")), singleton("reserved")));

	}

}
