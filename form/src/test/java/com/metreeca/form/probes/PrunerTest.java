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

package com.metreeca.form.probes;

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Test.test;

import static org.assertj.core.api.Assertions.assertThat;


final class PrunerTest {

	@Test void testConstraint() {

		assertThat((Object)MinCount.minCount(1)).as("MinCount").isEqualTo(prune(MinCount.minCount(1)));
		assertThat((Object)maxCount(1)).as("MaxCount").isEqualTo(prune(maxCount(1)));

		assertThat((Object)Clazz.clazz(RDF.NIL)).as("Clazz").isEqualTo(prune(Clazz.clazz(RDF.NIL)));
		assertThat((Object)Datatype.datatype(RDF.NIL)).as("Type").isEqualTo(prune(Datatype.datatype(RDF.NIL)));

		assertThat((Object)All.all(RDF.NIL)).as("Universal").isEqualTo(prune(All.all(RDF.NIL)));
		assertThat((Object)Any.any(RDF.NIL)).as("Universal").isEqualTo(prune(Any.any(RDF.NIL)));

		assertThat((Object)MinInclusive.minInclusive(RDF.NIL)).as("MinInclusive").isEqualTo(prune(MinInclusive.minInclusive(RDF.NIL)));
		assertThat((Object)MaxInclusive.maxInclusive(RDF.NIL)).as("MaxInclusive").isEqualTo(prune(MaxInclusive.maxInclusive(RDF.NIL)));
		assertThat((Object)MinExclusive.minExclusive(RDF.NIL)).as("MinExclusive").isEqualTo(prune(MinExclusive.minExclusive(RDF.NIL)));
		assertThat((Object)MaxExclusive.maxExclusive(RDF.NIL)).as("MaxExclusive").isEqualTo(prune(MaxExclusive.maxExclusive(RDF.NIL)));

		assertThat((Object)Pattern.pattern("pattern")).as("Pattern").isEqualTo(prune(Pattern.pattern("pattern")));
		assertThat((Object)Like.like("pattern")).as("Like").isEqualTo(prune(Like.like("pattern")));
		assertThat((Object)MinLength.minLength(1)).as("MinLength").isEqualTo(prune(MinLength.minLength(1)));
		assertThat((Object)MaxLength.maxLength(1)).as("MaxLength").isEqualTo(prune(MaxLength.maxLength(1)));

	}

	@Test void testTrait() {

		assertThat((Object)and()).as("dead").isEqualTo(prune(Trait.trait(RDF.VALUE)));
		assertThat((Object)Trait.trait(RDF.VALUE, maxCount(1))).as("live").isEqualTo(prune(Trait.trait(RDF.VALUE, maxCount(1))));

	}

	@Test void testVirtual() {

		assertThat((Object)and()).as("dead").isEqualTo(prune(Virtual.virtual(Trait.trait(RDF.VALUE), Step.step(RDF.NIL))));

		assertThat((Object)Virtual.virtual(Trait.trait(RDF.VALUE, maxCount(1)), Step.step(RDF.NIL))).as("live").isEqualTo(prune(Virtual.virtual(Trait.trait(RDF.VALUE, maxCount(1)), Step.step(RDF.NIL))));

	}

	@Test void testConjunction() {

		assertThat((Object)and()).as("empty").isEqualTo(prune(and()));
		assertThat((Object)and()).as("dead").isEqualTo(prune(and(and())));

		assertThat((Object)and(maxCount(1))).as("live").isEqualTo(prune(and(maxCount(1))));

	}

	@Test void testDisjunction() {

		assertThat((Object)and()).as("empty").isEqualTo(prune(or()));
		assertThat((Object)and()).as("dead").isEqualTo(prune(or(or())));

		assertThat((Object)or(maxCount(1))).as("live").isEqualTo(prune(or(maxCount(1))));

	}

	@Test void testOptions() {

		assertThat((Object)and()).as("empty").isEqualTo(prune(or()));
		assertThat((Object)and()).as("dead").isEqualTo(prune(or(or())));

		// test shape is not pruned
		assertThat((Object)test(or(), maxCount(1), and())).as("live").isEqualTo(prune(test(or(), maxCount(1), or())));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape prune(final Shape shape) {
		return shape.accept(new Pruner());
	}

}
