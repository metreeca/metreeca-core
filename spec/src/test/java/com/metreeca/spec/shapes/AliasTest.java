/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.spec.shapes;

import com.metreeca.spec.shifts.Step;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Test;

import static com.metreeca.spec.shapes.Alias.aliases;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Group.group;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shapes.Virtual.virtual;
import static com.metreeca.spec.things.Maps.entry;
import static com.metreeca.spec.things.Maps.map;
import static com.metreeca.spec.things.Values.iri;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;


public class AliasTest {

	private static final Step Value=Step.step(RDF.VALUE);


	@Test public void testGuessAliasFromIRI() {

		assertEquals("direct",
				singletonMap(Value, "value"),
				aliases(trait(Value)));

		assertEquals("inverse",
				singletonMap(Step.step(RDF.VALUE, true), "valueOf"),
				aliases(trait(Step.step(RDF.VALUE, true))));

	}

	@Test public void testRetrieveUserDefinedAlias() {
		assertEquals("user-defined",
				singletonMap(Value, "alias"),
				aliases(trait(Value, Alias.alias("alias"))));
	}

	@Test public void testPreferUserDefinedAliases() {
		assertEquals("user-defined", map(entry(Value, "alias")), aliases(and(trait(Value, Alias.alias("alias")), trait(Value))));
	}


	@Test public void testRetrieveAliasFromNestedShapes() {

		assertEquals("group",
				map(entry(Value, "alias")),
				aliases(group(trait(Value, Alias.alias("alias")))));

		assertEquals("system-guessed virtual",
				map(entry(Value, "value")),
				aliases(virtual(trait(Value), Step.step(RDF.NIL))));

		assertEquals("user-defined virtual",
				map(entry(Value, "alias")),
				aliases(virtual(trait(Value, Alias.alias("alias")), Step.step(RDF.NIL))));

		assertEquals("conjunction",
				map(entry(Value, "alias")),
				aliases(trait(Value, and(Alias.alias("alias")))));

	}

	@Test public void testMergeDuplicateTraits() {

		assertEquals("system-guessed",
				map(entry(Value, "value")),
				// nesting required to prevent and() from collapsing duplicates
				aliases(and(trait(Value), and(trait(Value)))));

		assertEquals("user-defined",
				map(entry(Value, "alias")),
				// nesting required to prevent and() from collapsing duplicates
				aliases(and(trait(Value, Alias.alias("alias")), and(trait(Value, Alias.alias("alias"))))));

	}


	@Test public void testHandleMultipleAliases() {

		assertEquals("clashing",
				map(entry(Value, "value")),
				aliases(trait(Value, and(Alias.alias("one"), Alias.alias("two")))));

		assertEquals("repeated",
				map(entry(Value, "one")),
				aliases(trait(Value, and(Alias.alias("one"), Alias.alias("one")))));

	}

	@Test public void testMergeAliases() {
		assertEquals("merged",
				map(entry(Step.step(RDF.TYPE), "type"), entry(Value, "value")),
				aliases(and(trait(RDF.TYPE), trait(Value))));
	}

	@Test public void testIgnoreClashingAliases() {

		assertTrue("different traits",
				aliases(and(trait(Value), trait(iri("urn:example:value")))).isEmpty());

		assertEquals("same trait",
				map(entry(Value, "value")), // fall back to system-guess alias
				aliases(and(trait(Value, Alias.alias("one")), trait(Value, Alias.alias("two")))));

	}

	@Test public void testIgnoreReservedAliases() {

		assertTrue("ignore reserved system-guessed aliases",
				aliases(trait(Value), singleton("value")).isEmpty());

		assertEquals("ignore reserved user-defined aliases",
				singletonMap(Value, "value"),
				aliases(trait(Value, Alias.alias("reserved")), singleton("reserved")));

	}

}
