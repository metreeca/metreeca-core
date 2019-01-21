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

package com.metreeca.form.shapes;

import com.metreeca.form.shifts.Step;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Meta.alias;
import static com.metreeca.form.shapes.Meta.aliases;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.shapes.Virtual.virtual;
import static com.metreeca.form.shifts.Step.step;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Values.iri;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;


final class MetaTest {

	private final Step Value=step(RDF.VALUE);


	@Nested final class AliasTest {

		@Test void testGuessAliasFromIRI() {

			assertThat(singletonMap(Value, "value"))
					.as("direct")
					.isEqualTo(aliases(trait(Value)));

			assertThat(singletonMap(step(RDF.VALUE, true), "valueOf"))
					.as("inverse")
					.isEqualTo(aliases(trait(step(RDF.VALUE, true))));

		}

		@Test void testRetrieveUserDefinedAlias() {
			assertThat(singletonMap(Value, "alias"))
					.as("user-defined")
					.isEqualTo(aliases(trait(Value, alias("alias"))));
		}

		@Test void testPreferUserDefinedAliases() {
			assertThat(map(entry(Value, "alias"))).as("user-defined").isEqualTo(aliases(and(trait(Value, alias("alias")), trait(Value))));
		}


		@Test void testRetrieveAliasFromNestedShapes() {

			assertThat(map(entry(Value, "alias"))).as("group").isEqualTo(aliases(and(trait(Value, alias("alias")))));

			assertThat(map(entry(Value, "value"))).as("system-guessed virtual").isEqualTo(aliases(virtual(trait(Value), step(RDF.NIL))));

			assertThat(map(entry(Value, "alias"))).as("user-defined virtual").isEqualTo(aliases(virtual(trait(Value, alias("alias")), step(RDF.NIL))));

			assertThat(map(entry(Value, "alias"))).as("conjunction").isEqualTo(aliases(trait(Value, and(alias("alias")))));

		}

		@Test void testMergeDuplicateTraits() {

			// nesting required to prevent and() from collapsing duplicates
			assertThat(map(entry(Value, "value"))).as("system-guessed").isEqualTo(aliases(and(trait(Value), and(trait(Value)))));

			// nesting required to prevent and() from collapsing duplicates
			assertThat(map(entry(Value, "alias"))).as("user-defined")
					.isEqualTo(aliases(and(trait(Value, alias("alias")), and(trait(Value, alias("alias"))))));

		}


		@Test void testHandleMultipleAliases() {

			assertThat(map(entry(Value, "value"))).as("clashing").isEqualTo(aliases(trait(Value, and(alias("one"), alias("two")))));

			assertThat(map(entry(Value, "one"))).as("repeated").isEqualTo(aliases(trait(Value, and(alias("one"), alias("one")))));

		}

		@Test void testMergeAliases() {
			assertThat(map(entry(step(RDF.TYPE), "type"), entry(Value, "value")))
					.as("merged")
					.isEqualTo(aliases(and(trait(RDF.TYPE), trait(Value))));
		}

		@Test void testIgnoreClashingAliases() {

			assertThat(aliases(and(trait(Value), trait(iri("urn:example:value")))).isEmpty()).as("different traits").isTrue();

			// fall back to system-guess alias
			assertThat(map(entry(Value, "value"))).as("same trait")
					.isEqualTo(aliases(and(trait(Value, alias("one")), trait(Value, alias("two")))));

		}

		@Test void testIgnoreReservedAliases() {

			assertThat(aliases(trait(Value), singleton("value")).isEmpty()).as("ignore reserved system-guessed aliases").isTrue();

			assertThat(singletonMap(Value, "value")).as("ignore reserved user-defined aliases").isEqualTo(aliases(trait(Value, alias("reserved")), singleton("reserved")));

		}

	}

}
