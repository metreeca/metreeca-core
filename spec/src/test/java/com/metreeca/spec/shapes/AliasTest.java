/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec.shapes;

import com.metreeca.spec.shifts.Step;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Test;

import static com.metreeca.jeep.Jeep.entry;
import static com.metreeca.jeep.Jeep.map;
import static com.metreeca.jeep.rdf.Values.iri;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Group.group;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shapes.Virtual.virtual;

import static org.junit.Assert.assertTrue;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;


public class AliasTest {

	private static final Step Value=Step.step(RDF.VALUE);


	@Test public void testGuessAliasFromIRI() {

		Assert.assertEquals("direct",
				singletonMap(Value, "value"),
				Alias.aliases(trait(Value)));

		Assert.assertEquals("inverse",
				singletonMap(Step.step(RDF.VALUE, true), "valueOf"),
				Alias.aliases(trait(Step.step(RDF.VALUE, true))));

	}

	@Test public void testRetrieveUserDefinedAlias() {
		Assert.assertEquals("user-defined",
				singletonMap(Value, "alias"),
				Alias.aliases(trait(Value, Alias.alias("alias"))));
	}

	@Test public void testPreferUserDefinedAliases() {
		Assert.assertEquals("user-defined", map(entry(Value, "alias")), Alias.aliases(And.and(trait(Value, Alias.alias("alias")), trait(Value))));
	}


	@Test public void testRetrieveAliasFromNestedShapes() {

		Assert.assertEquals("group",
				map(entry(Value, "alias")),
				Alias.aliases(group(trait(Value, Alias.alias("alias")))));

		Assert.assertEquals("system-guessed virtual",
				map(entry(Value, "value")),
				Alias.aliases(virtual(trait(Value), Step.step(RDF.NIL))));

		Assert.assertEquals("user-defined virtual",
				map(entry(Value, "alias")),
				Alias.aliases(virtual(trait(Value, Alias.alias("alias")), Step.step(RDF.NIL))));

		Assert.assertEquals("conjunction",
				map(entry(Value, "alias")),
				Alias.aliases(trait(Value, And.and(Alias.alias("alias")))));

	}

	@Test public void testMergeDuplicateTraits() {

		Assert.assertEquals("system-guessed",
				map(entry(Value, "value")),
				// nesting required to prevent and() from collapsing duplicates
				Alias.aliases(And.and(trait(Value), and(trait(Value)))));

		Assert.assertEquals("user-defined",
				map(entry(Value, "alias")),
				// nesting required to prevent and() from collapsing duplicates
				Alias.aliases(And.and(trait(Value, Alias.alias("alias")), and(trait(Value, Alias.alias("alias"))))));

	}


	@Test public void testHandleMultipleAliases() {

		Assert.assertEquals("clashing",
				map(entry(Value, "value")),
				Alias.aliases(trait(Value, And.and(Alias.alias("one"), Alias.alias("two")))));

		Assert.assertEquals("repeated",
				map(entry(Value, "one")),
				Alias.aliases(trait(Value, And.and(Alias.alias("one"), Alias.alias("one")))));

	}

	@Test public void testMergeAliases() {
		Assert.assertEquals("merged",
				map(entry(Step.step(RDF.TYPE), "type"), entry(Value, "value")),
				Alias.aliases(And.and(trait(RDF.TYPE), trait(Value))));
	}

	@Test public void testIgnoreClashingAliases() {

		assertTrue("different traits",
				Alias.aliases(And.and(trait(Value), trait(iri("urn:example:value")))).isEmpty());

		Assert.assertEquals("same trait",
				map(entry(Value, "value")), // fall back to system-guess alias
				Alias.aliases(And.and(trait(Value, Alias.alias("one")), trait(Value, Alias.alias("two")))));

	}

	@Test public void testIgnoreReservedAliases() {

		assertTrue("ignore reserved system-guessed aliases",
				Alias.aliases(trait(Value), singleton("value")).isEmpty());

		Assert.assertEquals("ignore reserved user-defined aliases",
				singletonMap(Value, "value"),
				Alias.aliases(trait(Value, Alias.alias("reserved")), singleton("reserved")));

	}

}
