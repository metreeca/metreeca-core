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

package com.metreeca.form.shapes;

import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Maps;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Test;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.shapes.Trait.traits;
import static com.metreeca.form.shapes.Virtual.virtual;
import static com.metreeca.form.things.Maps.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.util.Collections.singletonMap;


public class TraitTest {

	@Test public void testInspectTraits() {

		final Trait trait=trait(RDF.VALUE, and());

		assertEquals("singleton trait map", singletonMap(trait.getStep(), trait.getShape()), traits(trait));

	}

	@Test public void testInspectVirtuals() {

		final Virtual virtual=virtual(trait(RDF.VALUE), Step.step(RDF.NIL));

		assertEquals("singleton trait map", traits(virtual.getTrait()), traits(virtual));

	}

	@Test public void testInspectConjunctions() {

		final Trait x=trait(RDF.VALUE, and());
		final Trait y=trait(RDF.TYPE, and());
		final Trait z=trait(RDF.TYPE, maxCount(1));

		assertEquals("union trait map", Maps.map(
				Maps.entry(x.getStep(), x.getShape()),
				Maps.entry(y.getStep(), y.getShape())
		), traits(and(x, y)));

		assertEquals("merged trait map", map(
				Maps.entry(y.getStep(), and(y.getShape(), z.getShape()))
		), traits(and(y, z)));

	}

	@Test public void testInspectOtherShapes() {
		assertTrue("no traits", traits(and()).isEmpty());
	}

}
