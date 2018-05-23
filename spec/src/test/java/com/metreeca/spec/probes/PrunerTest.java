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

package com.metreeca.spec.probes;

import com.metreeca.spec.Shape;
import com.metreeca.spec.shapes.*;
import com.metreeca.spec.shifts.Step;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Assert;

import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.MaxCount.maxCount;
import static com.metreeca.spec.shapes.Or.or;

import static org.junit.Assert.assertEquals;


public class PrunerTest {

	@org.junit.Test public void testConstraint() {

		Assert.assertEquals("MinCount", MinCount.minCount(1), prune(MinCount.minCount(1)));
		assertEquals("MaxCount", maxCount(1), prune(maxCount(1)));

		Assert.assertEquals("Clazz", Clazz.clazz(RDF.NIL), prune(Clazz.clazz(RDF.NIL)));
		Assert.assertEquals("Type", Datatype.datatype(RDF.NIL), prune(Datatype.datatype(RDF.NIL)));

		assertEquals("Universal", All.all(RDF.NIL), prune(All.all(RDF.NIL)));
		assertEquals("Universal", Any.any(RDF.NIL), prune(Any.any(RDF.NIL)));

		Assert.assertEquals("MinInclusive", MinInclusive.minInclusive(RDF.NIL), prune(MinInclusive.minInclusive(RDF.NIL)));
		Assert.assertEquals("MaxInclusive", MaxInclusive.maxInclusive(RDF.NIL), prune(MaxInclusive.maxInclusive(RDF.NIL)));
		Assert.assertEquals("MinExclusive", MinExclusive.minExclusive(RDF.NIL), prune(MinExclusive.minExclusive(RDF.NIL)));
		Assert.assertEquals("MaxExclusive", MaxExclusive.maxExclusive(RDF.NIL), prune(MaxExclusive.maxExclusive(RDF.NIL)));

		Assert.assertEquals("Pattern", Pattern.pattern("pattern"), prune(Pattern.pattern("pattern")));
		Assert.assertEquals("Like", Like.like("pattern"), prune(Like.like("pattern")));
		Assert.assertEquals("MinLength", MinLength.minLength(1), prune(MinLength.minLength(1)));
		Assert.assertEquals("MaxLength", MaxLength.maxLength(1), prune(MaxLength.maxLength(1)));

	}

	@org.junit.Test public void testTrait() {

		Assert.assertEquals("dead", And.and(), prune(Trait.trait(RDF.VALUE)));
		assertEquals("live", Trait.trait(RDF.VALUE, maxCount(1)), prune(Trait.trait(RDF.VALUE, maxCount(1))));

	}

	@org.junit.Test public void testVirtual() {

		Assert.assertEquals("dead", And.and(), prune(Virtual.virtual(Trait.trait(RDF.VALUE), Step.step(RDF.NIL))));

		Assert.assertEquals("live", Virtual.virtual(Trait.trait(RDF.VALUE, maxCount(1)), Step.step(RDF.NIL)),
				prune(Virtual.virtual(Trait.trait(RDF.VALUE, maxCount(1)), Step.step(RDF.NIL))));

	}

	@org.junit.Test public void testConjunction() {

		Assert.assertEquals("empty", And.and(), prune(And.and()));
		Assert.assertEquals("dead", And.and(), prune(And.and(And.and())));

		Assert.assertEquals("live", and(maxCount(1)), prune(and(maxCount(1))));

	}

	@org.junit.Test public void testDisjunction() {

		Assert.assertEquals("empty", And.and(), prune(Or.or()));
		Assert.assertEquals("dead", And.and(), prune(Or.or(Or.or())));

		Assert.assertEquals("live", or(maxCount(1)), prune(or(maxCount(1))));

	}

	@org.junit.Test public void testOptions() {

		Assert.assertEquals("empty", And.and(), prune(Or.or()));
		Assert.assertEquals("dead", And.and(), prune(Or.or(Or.or())));

		Assert.assertEquals("live", Test.test(Or.or(), maxCount(1), And.and()), prune(Test.test(Or.or(), maxCount(1), Or.or()))); // test shape is not pruned

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape prune(final Shape shape) {
		return shape.accept(new Pruner());
	}

}
