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

package com.metreeca.spec.probes;

import com.metreeca.jeep.rdf.Values;
import com.metreeca.spec.Shape;
import com.metreeca.spec.shapes.Datatype;
import com.metreeca.spec.shapes.MinCount;
import com.metreeca.spec.shifts.Step;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.Test;

import static com.metreeca.spec.shapes.Alias.alias;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.MaxCount.maxCount;
import static com.metreeca.spec.shapes.Or.or;
import static com.metreeca.spec.shapes.Test.test;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shapes.Virtual.virtual;

import static org.junit.Assert.assertEquals;


public class OptimizerTest {

	private static final Shape x=Datatype.datatype(RDF.NIL);
	private static final Shape y=MinCount.minCount(1);
	private static final Shape z=maxCount(10);


	@Test public void testRetainAliases() { // required by formatters
		assertEquals("alias", alias("alias"), optimize(alias("alias")));
	}


	@Test public void testOptimizeMinCount() {

		assertEquals("conjunction", MinCount.minCount(100), optimize(and(MinCount.minCount(10), MinCount.minCount(100))));
		assertEquals("disjunction", MinCount.minCount(10), optimize(or(MinCount.minCount(10), MinCount.minCount(100))));

	}

	@Test public void testOptimizeMaxCount() {

		assertEquals("conjunction", maxCount(10), optimize(and(maxCount(10), maxCount(100))));
		assertEquals("disjunction", maxCount(100), optimize(or(maxCount(10), maxCount(100))));

	}


	@Test public void testOptimizeType() {

		assertEquals("conjunction / superclass",
				Datatype.datatype(Values.IRIType), optimize(and(Datatype.datatype(Values.IRIType), Datatype.datatype(Values.ResoureType))));
		assertEquals("conjunction / literal",
				Datatype.datatype(XMLSchema.STRING), optimize(and(Datatype.datatype(Values.LiteralType), Datatype.datatype(XMLSchema.STRING))));
		assertEquals("conjunction / unrelated",
				and(Datatype.datatype(Values.ResoureType), Datatype.datatype(XMLSchema.STRING)), optimize(and(Datatype.datatype(Values.ResoureType), Datatype.datatype(XMLSchema.STRING))));

		assertEquals("disjunction / superclass",
				Datatype.datatype(Values.ResoureType), optimize(or(Datatype.datatype(Values.IRIType), Datatype.datatype(Values.ResoureType))));
		assertEquals("disjunction / literal",
				Datatype.datatype(Values.LiteralType), optimize(or(Datatype.datatype(Values.LiteralType), Datatype.datatype(RDF.NIL))));
		assertEquals("disjunction / unrelated",
				or(Datatype.datatype(Values.ResoureType), Datatype.datatype(RDF.NIL)), optimize(or(Datatype.datatype(Values.ResoureType), Datatype.datatype(RDF.NIL))));

	}


	@Test public void testOptimizeTraits() {

		assertEquals("optimize nested shape", trait(RDF.VALUE, x), optimize(trait(RDF.VALUE, and(x))));
		assertEquals("remove dead traits", and(), optimize(trait(RDF.VALUE, or())));

		assertEquals("merge conjunctive traits",
				and(alias("alias"), trait(RDF.VALUE, and(MinCount.minCount(1), maxCount(3)))),
				optimize(and(alias("alias"), trait(RDF.VALUE, MinCount.minCount(1)), trait(RDF.VALUE, maxCount(3)))));

		assertEquals("merge disjunctive traits",
				or(alias("alias"), trait(RDF.VALUE, or(MinCount.minCount(1), maxCount(3)))),
				optimize(or(alias("alias"), trait(RDF.VALUE, MinCount.minCount(1)), trait(RDF.VALUE, maxCount(3)))));

	}

	@Test public void testOptimizeVirtuals() {

		assertEquals("optimize nested shape",
				virtual(trait(RDF.VALUE, x), Step.step(RDF.NIL)),
				optimize(virtual(trait(RDF.VALUE, and(x)), Step.step(RDF.NIL))));

		assertEquals("remove dead traits", and(),
				optimize(virtual(trait(RDF.VALUE, or()), Step.step(RDF.NIL))));

	}


	@Test public void testOptimizeConjunctions() {

		assertEquals("simplify constants", or(), optimize(and(or(), trait(RDF.TYPE))));
		assertEquals("unwrap singletons", x, optimize(and(x)));
		assertEquals("unwrap unique values", x, optimize(and(x, x)));
		assertEquals("remove duplicates", and(x, y), optimize(and(x, x, y)));
		assertEquals("merge nested conjunction", and(x, y, z), optimize(and(and(x), and(y, z))));

	}

	@Test public void testOptimizeDisjunctions() {

		assertEquals("simplify constants", and(), optimize(or(and(), trait(RDF.TYPE))));
		assertEquals("unwrap singletons", x, optimize(or(x)));
		assertEquals("unwrap unique values", x, optimize(or(x, x)));
		assertEquals("remove duplicates", or(x, y), optimize(or(x, x, y)));
		assertEquals("merge nested disjunctions", or(x, y, z), optimize(or(or(x), or(y, z))));

	}

	@Test public void testOptimizeOption() {

		assertEquals("always pass", x, optimize(test(and(), x, y)));
		assertEquals("always fail", y, optimize(test(or(), x, y)));

		assertEquals("identical options", y, optimize(test(x, y, y)));

		assertEquals("optimized test shape", test(x, y, z), optimize(test(and(x), y, z)));
		assertEquals("optimized pass shape", test(x, y, z), optimize(test(x, and(y), z)));
		assertEquals("optimized fail shape", test(x, y, z), optimize(test(x, y, and(z))));

		assertEquals("material", test(x, y, z), optimize(test(x, y, z)));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape optimize(final Shape shape) {
		return shape.accept(new Optimizer());
	}

}
