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

import com.metreeca.form.Shift;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Meta.alias;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.Shift.shift;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Values.iri;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;


final class BaseCodecTest {

	private final Shift Value=Shift.shift(RDF.VALUE);


	@Test void testGuessAliasFromIRI() {

		assertThat(singletonMap(Value, "value"))
				.as("direct")
				.isEqualTo(BaseCodec.aliases(trait(Value)));

		assertThat(singletonMap(shift(RDF.VALUE).inverse(), "valueOf"))
				.as("inverse")
				.isEqualTo(BaseCodec.aliases(trait(shift(RDF.VALUE).inverse())));

	}

	@Test void testRetrieveUserDefinedAlias() {
		assertThat(singletonMap(Value, "alias"))
				.as("user-defined")
				.isEqualTo(BaseCodec.aliases(trait(Value, alias("alias"))));
	}

	@Test void testPreferUserDefinedAliases() {
		assertThat(map(entry(Value, "alias"))).as("user-defined").isEqualTo(BaseCodec.aliases(and(trait(Value, alias("alias")), trait(Value))));
	}


	@Test void testRetrieveAliasFromNestedShapes() {

		assertThat(map(entry(Value, "alias"))).as("group").isEqualTo(BaseCodec.aliases(and(trait(Value, alias("alias")))));

		assertThat(map(entry(Value, "alias"))).as("conjunction").isEqualTo(BaseCodec.aliases(trait(Value, and(alias("alias")))));

	}

	@Test void testMergeDuplicateTraits() {

		// nesting required to prevent and() from collapsing duplicates
		assertThat(map(entry(Value, "value"))).as("system-guessed").isEqualTo(BaseCodec.aliases(and(trait(Value), and(trait(Value)))));

		// nesting required to prevent and() from collapsing duplicates
		assertThat(map(entry(Value, "alias"))).as("user-defined")
				.isEqualTo(BaseCodec.aliases(and(trait(Value, alias("alias")), and(trait(Value, alias("alias"))))));

	}


	@Test void testHandleMultipleAliases() {

		assertThat(map(entry(Value, "value"))).as("clashing").isEqualTo(BaseCodec.aliases(trait(Value, and(alias("one"), alias("two")))));

		assertThat(map(entry(Value, "one"))).as("repeated").isEqualTo(BaseCodec.aliases(trait(Value, and(alias("one"), alias("one")))));

	}

	@Test void testMergeAliases() {
		assertThat(map(entry(Shift.shift(RDF.TYPE), "type"), entry(Value, "value")))
				.as("merged")
				.isEqualTo(BaseCodec.aliases(and(trait(RDF.TYPE), trait(Value))));
	}

	@Test void testIgnoreClashingAliases() {

		assertThat(BaseCodec.aliases(and(trait(Value), trait(iri("urn:example:value")))).isEmpty()).as("different traits").isTrue();

		// fall back to system-guess alias
		assertThat(map(entry(Value, "value"))).as("same trait")
				.isEqualTo(BaseCodec.aliases(and(trait(Value, alias("one")), trait(Value, alias("two")))));

	}

	@Test void testIgnoreReservedAliases() {

		assertThat(BaseCodec.aliases(trait(Value), singleton("value")).isEmpty()).as("ignore reserved system-guessed aliases").isTrue();

		assertThat(singletonMap(Value, "value")).as("ignore reserved user-defined aliases").isEqualTo(BaseCodec.aliases(trait(Value, alias("reserved")), singleton("reserved")));

	}

}
