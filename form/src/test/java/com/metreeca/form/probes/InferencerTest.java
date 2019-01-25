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
import com.metreeca.form.things.Lists;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Meta.hint;
import static com.metreeca.form.shapes.Option.option;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Values.inverse;
import static com.metreeca.form.things.Values.literal;

import static org.assertj.core.api.Assertions.assertThat;


final class InferencerTest {

	@Test void testHint() {

		assertImplies("hinted shapes are resources",
				hint(RDF.NIL), datatype(Values.ResourceType));

	}

	@Test void testUniversal() {
		assertImplies("minimum focus size is equal to the size of the required value set",
				all(literal(1), literal(2)), MinCount.minCount(2));
	}

	@Test void testExistential() {
		assertImplies("minimum focus size is 1",
				any(literal(1), literal(2)), MinCount.minCount(1));
	}

	@Test void testType() { // !!! improve testing of multiple implications

		assertImplies("xsd:boolean has closed range",
				datatype(XMLSchema.BOOLEAN), and(maxCount(1), In.in(literal(false), literal(true))));

		assertImplies("xsd:boolean has exclusive values",
				datatype(XMLSchema.BOOLEAN), and(maxCount(1), In.in(literal(false), literal(true))));

	}

	@Test void testClazz() {
		assertImplies("classed values are resources",
				clazz(RDF.NIL), datatype(Values.ResourceType));
	}

	@Test void testRange() {

		assertImplies("maximum focus size is equal to the size of the allowed value set",
				In.in(literal(1), literal(2.0)), maxCount(2));

		assertImplies("if unique, focus values share the datatype of the allowed value set",
				In.in(literal(1), literal(2)), and(maxCount(2), datatype(XMLSchema.INT)));

	}

	@Test void testTrait() {

		assertImplies("trait subjects are resources",
				trait(RDF.VALUE), datatype(Values.ResourceType));

		assertImplies("reverse trait objects are resources",
				trait(inverse(RDF.VALUE)), datatype(Values.ResourceType), (s, i) -> trait(s.getIRI(), and(s.getShape(), i)));

		assertImplies("both subject and object of a rdf:type trait are resources",
				trait(RDF.TYPE), datatype(Values.ResourceType),
				(s, i) -> and(trait(s.getIRI(), and(s.getShape(), i)), i));

		assertImplies("nested shapes are expanded",
				trait(RDF.VALUE, clazz(RDF.NIL)), datatype(Values.ResourceType),
				(s, i) -> and(trait(s.getIRI(), and(and(s.getShape(), i), datatype(Values.ResourceType))), datatype(Values.ResourceType)));
	}

	@Test void testConjunction() {
		assertImplies("nested shapes are expanded", and(clazz(RDF.NIL)), datatype(Values.ResourceType),
				(s, i) -> and(Lists.concat(s.getShapes(), list(i)))); // outer and() stripped by optimization
	}

	@Test void testDisjunction() {
		assertImplies("nested shapes are expanded", Or.or(clazz(RDF.NIL)), datatype(Values.ResourceType),
				(s, i) -> and(Lists.concat(s.getShapes(), list(i)))); // outer or() stripped by optimization
	}

	@Test void testOption() {
		assertImplies("nested shapes are expanded",
				option(clazz(RDF.NIL), clazz(RDF.NIL), clazz(RDF.NIL)), datatype(Values.ResourceType),
				(s, i) -> option(and(s.getTest(), i), and(s.getPass(), i), and(s.getFail(), i)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void assertImplies(final String message, final Shape shape, final Shape inferred) {
		assertThat(expand(shape)).as(message).isEqualTo(optimize(and(shape, inferred)));
	}

	private <S extends Shape, I extends Shape> void assertImplies(
			final String message, final S shape, final I inferred, final BiFunction<S, I, Shape> mapper) {
		assertThat(expand(shape)).as(message).isEqualTo(optimize(mapper.apply(shape, inferred)));
	}


	private <S extends Shape, I extends Shape> Shape optimize(final Shape shape) {
		return shape.map(new Optimizer());
	}

	private Shape expand(final Shape shape) {
		return optimize(shape.map(new Inferencer()));
	}

}
