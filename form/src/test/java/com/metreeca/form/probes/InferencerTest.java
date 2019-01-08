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

package com.metreeca.form.probes;

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Lists;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Test.test;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Values.literal;

import static org.assertj.core.api.Assertions.assertThat;


final class InferencerTest {

	@Test void testHint() {

		assertImplies("hinted shapes are resources",
				Hint.hint(RDF.NIL), Datatype.datatype(Values.ResoureType));

	}

	@Test void testUniversal() {
		assertImplies("minimum focus size is equal to the size of the required value set",
				All.all(literal(1), literal(2)), MinCount.minCount(2));
	}

	@Test void testExistential() {
		assertImplies("minimum focus size is 1",
				Any.any(literal(1), literal(2)), MinCount.minCount(1));
	}

	@Test void testType() { // !!! improve testing of multiple implications

		assertImplies("xsd:boolean has closed range",
				Datatype.datatype(XMLSchema.BOOLEAN), and(maxCount(1), In.in(literal(false), literal(true))));

		assertImplies("xsd:boolean has exclusive values",
				Datatype.datatype(XMLSchema.BOOLEAN), and(maxCount(1), In.in(literal(false), literal(true))));

	}

	@Test void testClazz() {
		assertImplies("classed values are resources",
				clazz(RDF.NIL), Datatype.datatype(Values.ResoureType));
	}

	@Test void testRange() {

		assertImplies("maximum focus size is equal to the size of the allowed value set",
				In.in(literal(1), literal(2.0)), maxCount(2));

		assertImplies("if unique, focus values share the datatype of the allowed value set",
				In.in(literal(1), literal(2)), and(maxCount(2), Datatype.datatype(XMLSchema.INT)));

	}

	@Test void testTrait() {

		assertImplies("trait subjects are resources",
				trait(Step.step(RDF.VALUE)), Datatype.datatype(Values.ResoureType));

		assertImplies("reverse trait objects are resources",
				trait(Step.step(RDF.VALUE, true)), Datatype.datatype(Values.ResoureType), (s, i) -> trait(s.getStep(), and(s.getShape(), i)));

		assertImplies("both subject and object of a rdf:type trait are resources",
				trait(Step.step(RDF.TYPE)), Datatype.datatype(Values.ResoureType),
				(s, i) -> and(trait(s.getStep(), and(s.getShape(), i)), i));

		assertImplies("nested shapes are expanded",
				trait(RDF.VALUE, clazz(RDF.NIL)), Datatype.datatype(Values.ResoureType),
				(s, i) -> and(trait(s.getStep(), and(and(s.getShape(), i), Datatype.datatype(Values.ResoureType))), Datatype.datatype(Values.ResoureType)));
	}

	@Disabled @Test void testVirtual() {

		assertImplies("virtual traits are expanded",
				Virtual.virtual(trait(RDF.VALUE), Step.step(RDF.NIL)), Datatype.datatype(Values.ResoureType),
				(s, i) -> Virtual.virtual(trait(RDF.VALUE, i), Step.step(RDF.NIL)));

	}


	@Test void testConjunction() {
		assertImplies("nested shapes are expanded", and(clazz(RDF.NIL)), Datatype.datatype(Values.ResoureType),
				(s, i) -> and(Lists.concat(s.getShapes(), list(i)))); // outer and() stripped by optimization
	}

	@Test void testDisjunction() {
		assertImplies("nested shapes are expanded", Or.or(clazz(RDF.NIL)), Datatype.datatype(Values.ResoureType),
				(s, i) -> and(Lists.concat(s.getShapes(), list(i)))); // outer or() stripped by optimization
	}

	@Test void testOption() {
		assertImplies("nested shapes are expanded",
				test(clazz(RDF.NIL), clazz(RDF.NIL), clazz(RDF.NIL)), Datatype.datatype(Values.ResoureType),
				(s, i) -> test(and(s.getTest(), i), and(s.getPass(), i), and(s.getFail(), i)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void assertImplies(final String message, final Shape shape, final Shape inferred) {
		assertThat((Object)optimize(and(shape, inferred))).as(message).isEqualTo(expand(shape));
	}

	private <S extends Shape, I extends Shape> void assertImplies(
			final String message, final S shape, final I inferred, final BiFunction<S, I, Shape> mapper) {
		assertThat((Object)optimize(mapper.apply(shape, inferred))).as(message).isEqualTo(expand(shape));
	}


	private <S extends Shape, I extends Shape> Shape optimize(final Shape shape) {
		return shape.accept(new Optimizer());
	}

	private Shape expand(final Shape shape) {
		return optimize(shape.accept(new Inferencer()));
	}

}
