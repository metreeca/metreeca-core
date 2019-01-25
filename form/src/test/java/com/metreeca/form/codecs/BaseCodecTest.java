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

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.codecs.BaseCodec.aliases;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Meta.alias;
import static com.metreeca.form.shapes.Trait.trait;
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
				.isEqualTo(aliases(trait(RDF.VALUE)));

		assertThat(singletonMap(inverse(RDF.VALUE), "valueOf"))
				.as("inverse")
				.isEqualTo(aliases(trait(inverse(RDF.VALUE))));

	}

	@Test void testRetrieveUserDefinedAlias() {
		assertThat(singletonMap(RDF.VALUE, "alias"))
				.as("user-defined")
				.isEqualTo(aliases(trait(RDF.VALUE, alias("alias"))));
	}

	@Test void testPreferUserDefinedAliases() {
		assertThat(map(entry(RDF.VALUE, "alias")))
				.as("user-defined")
				.isEqualTo(aliases(and(trait(RDF.VALUE, alias("alias")), trait(RDF.VALUE))));
	}


	@Test void testRetrieveAliasFromNestedShapes() {

		assertThat(map(entry(RDF.VALUE, "alias"))).as("group").isEqualTo(aliases(and(trait(RDF.VALUE, alias("alias")))));

		assertThat(map(entry(RDF.VALUE, "alias"))).as("conjunction").isEqualTo(aliases(trait(RDF.VALUE, and(alias("alias")))));

	}

	@Test void testMergeDuplicateTraits() {

		// nesting required to prevent and() from collapsing duplicates
		assertThat(map(entry(RDF.VALUE, "value"))).as("system-guessed").isEqualTo(aliases(and(trait(RDF.VALUE), and(trait(RDF.VALUE)))));

		// nesting required to prevent and() from collapsing duplicates
		assertThat(map(entry(RDF.VALUE, "alias"))).as("user-defined")
				.isEqualTo(aliases(and(trait(RDF.VALUE, alias("alias")), and(trait(RDF.VALUE, alias("alias"))))));

	}


	@Test void testHandleMultipleAliases() {

		assertThat(map(entry(RDF.VALUE, "value"))).as("clashing").isEqualTo(aliases(trait(RDF.VALUE, and(alias("one"), alias("two")))));

		assertThat(map(entry(RDF.VALUE, "one"))).as("repeated").isEqualTo(aliases(trait(RDF.VALUE, and(alias("one"), alias("one")))));

	}

	@Test void testMergeAliases() {
		assertThat(map(entry(RDF.TYPE, "type"), entry(RDF.VALUE, "value")))
				.as("merged")
				.isEqualTo(aliases(and(trait(RDF.TYPE), trait(RDF.VALUE))));
	}

	@Test void testIgnoreClashingAliases() {

		assertThat(aliases(and(trait(RDF.VALUE), trait(iri("urn:example:value")))).isEmpty()).as("different traits").isTrue();

		// fall back to system-guess alias
		assertThat(map(entry(RDF.VALUE, "value"))).as("same trait")
				.isEqualTo(aliases(and(trait(RDF.VALUE, alias("one")), trait(RDF.VALUE, alias("two")))));

	}

	@Test void testIgnoreReservedAliases() {

		assertThat(aliases(trait(RDF.VALUE), singleton("value")).isEmpty()).as("ignore reserved system-guessed aliases").isTrue();

		assertThat(singletonMap(RDF.VALUE, "value")).as("ignore reserved user-defined aliases").isEqualTo(aliases(trait(RDF.VALUE, alias("reserved")), singleton("reserved")));

	}

}
