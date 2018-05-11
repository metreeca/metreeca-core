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

import com.metreeca.spec.Shape;
import com.metreeca.spec.Values;
import com.metreeca.spec.shapes.*;
import com.metreeca.spec.shifts.Step;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.Ignore;

import java.util.function.BiFunction;

import static com.metreeca.jeep.Lists.concat;
import static com.metreeca.jeep.Lists.list;
import static com.metreeca.spec.Values.literal;
import static com.metreeca.spec.shapes.MaxCount.maxCount;
import static com.metreeca.spec.shapes.Trait.trait;

import static org.junit.Assert.assertEquals;


public final class InferencerTest {

	@org.junit.Test public void testHint() {

		assertImplies("hinted shapes are resources",
				Hint.hint(RDF.NIL), Datatype.datatype(Values.ResoureType));

	}

	@org.junit.Test public void testUniversal() {
		assertImplies("minimum focus size is equal to the size of the required value set",
				All.all(literal(1), literal(2)), MinCount.minCount(2));
	}

	@org.junit.Test public void testExistential() {
		assertImplies("minimum focus size is 1",
				Any.any(literal(1), literal(2)), MinCount.minCount(1));
	}

	@org.junit.Test public void testType() { // !!! improve testing of multiple implications

		assertImplies("xsd:boolean has closed range",
				Datatype.datatype(XMLSchema.BOOLEAN), And.and(maxCount(1), In.in(literal(false), literal(true))));

		assertImplies("xsd:boolean has exclusive values",
				Datatype.datatype(XMLSchema.BOOLEAN), And.and(maxCount(1), In.in(literal(false), literal(true))));

	}

	@org.junit.Test public void testClazz() {
		assertImplies("classed values are resources",
				Clazz.clazz(RDF.NIL), Datatype.datatype(Values.ResoureType));
	}

	@org.junit.Test public void testRange() {

		assertImplies("maximum focus size is equal to the size of the allowed value set",
				In.in(literal(1), literal(2.0)), maxCount(2));

		assertImplies("if unique, focus values share the datatype of the allowed value set",
				In.in(literal(1), literal(2)), And.and(maxCount(2), Datatype.datatype(XMLSchema.INT)));

	}

	@org.junit.Test public void testTrait() {

		assertImplies("trait subjects are resources",
				trait(Step.step(RDF.VALUE)), Datatype.datatype(Values.ResoureType));

		assertImplies("reverse trait objects are resources",
				trait(Step.step(RDF.VALUE, true)), Datatype.datatype(Values.ResoureType), (s, i) -> trait(s.getStep(), And.and(s.getShape(), i)));

		assertImplies("both subject and object of a rdf:type trait are resources",
				trait(Step.step(RDF.TYPE)), Datatype.datatype(Values.ResoureType),
				(s, i) -> And.and(trait(s.getStep(), And.and(s.getShape(), i)), i));

		assertImplies("nested shapes are expanded",
				trait(RDF.VALUE, Clazz.clazz(RDF.NIL)), Datatype.datatype(Values.ResoureType),
				(s, i) -> And.and(trait(s.getStep(), And.and(And.and(s.getShape(), i), Datatype.datatype(Values.ResoureType))), Datatype.datatype(Values.ResoureType)));
	}

	@Ignore @org.junit.Test public void testVirtual() {

		assertImplies("virtual traits are expanded",
				Virtual.virtual(trait(RDF.VALUE), Step.step(RDF.NIL)), Datatype.datatype(Values.ResoureType),
				(s, i) -> Virtual.virtual(trait(RDF.VALUE, i), Step.step(RDF.NIL)));

	}


	@org.junit.Test public void testConjunction() {
		assertImplies("nested shapes are expanded", And.and(Clazz.clazz(RDF.NIL)), Datatype.datatype(Values.ResoureType),
				(s, i) -> And.and(concat(s.getShapes(), list(i)))); // outer and() stripped by optimization
	}

	@org.junit.Test public void testDisjunction() {
		assertImplies("nested shapes are expanded", Or.or(Clazz.clazz(RDF.NIL)), Datatype.datatype(Values.ResoureType),
				(s, i) -> And.and(concat(s.getShapes(), list(i)))); // outer or() stripped by optimization
	}

	@org.junit.Test public void testOption() {
		assertImplies("nested shapes are expanded",
				Test.test(Clazz.clazz(RDF.NIL), Clazz.clazz(RDF.NIL), Clazz.clazz(RDF.NIL)), Datatype.datatype(Values.ResoureType),
				(s, i) -> Test.test(And.and(s.getTest(), i), And.and(s.getPass(), i), And.and(s.getFail(), i)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void assertImplies(final String message, final Shape shape, final Shape inferred) {
		assertEquals(message, optimize(And.and(shape, inferred)), expand(shape));
	}

	private <S extends Shape, I extends Shape> void assertImplies(
			final String message, final S shape, final I inferred, final BiFunction<S, I, Shape> mapper) {
		assertEquals(message, optimize(mapper.apply(shape, inferred)), expand(shape));
	}


	private <S extends Shape, I extends Shape> Shape optimize(final Shape shape) {
		return shape.accept(new Optimizer());
	}

	private Shape expand(final Shape shape) {
		return optimize(shape.accept(new Inferencer()));
	}

}
