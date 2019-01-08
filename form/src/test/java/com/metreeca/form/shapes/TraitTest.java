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
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.shapes.Trait.traits;
import static com.metreeca.form.shapes.Virtual.virtual;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.singletonMap;


final class TraitTest {

	@Test void testInspectTraits() {

		final Trait trait=trait(RDF.VALUE, and());

		assertThat(singletonMap(trait.getStep(), trait.getShape())).as("singleton trait map").isEqualTo(traits(trait));

	}

	@Test void testInspectVirtuals() {

		final Virtual virtual=virtual(trait(RDF.VALUE), Step.step(RDF.NIL));

		assertThat((Object)traits(virtual.getTrait())).as("singleton trait map").isEqualTo(traits(virtual));

	}

	@Test void testInspectConjunctions() {

		final Trait x=trait(RDF.VALUE, and());
		final Trait y=trait(RDF.TYPE, and());
		final Trait z=trait(RDF.TYPE, maxCount(1));

		assertThat(map(
				entry(x.getStep(), x.getShape()),
				entry(y.getStep(), y.getShape())
		)).as("union trait map").isEqualTo(traits(and(x, y)));

		assertThat(map(
				entry(y.getStep(), and(y.getShape(), z.getShape()))
		)).as("merged trait map").isEqualTo(traits(and(y, z)));

	}

	@Test void testInspectOtherShapes() {
		assertThat(traits(and()).isEmpty()).as("no traits").isTrue();
	}

}
