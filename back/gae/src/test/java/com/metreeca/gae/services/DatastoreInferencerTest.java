/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.gae.services;

import com.metreeca.gae.GAE;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Optimizer;

import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Any.any;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.In.in;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.Meta.hint;
import static com.metreeca.tree.shapes.MinCount.minCount;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.stream.Collectors.toList;


final class DatastoreInferencerTest {

	@Test void testHint() {

		assertImplies("hinted shapes are resources",
				hint("/entities/"), datatype(GAE.Entity));

	}

	@Test void testUniversal() {
		assertImplies("minimum focus size is equal to the size of the required value set",
				all(1, 2), minCount(2));
	}

	@Test void testExistential() {
		assertImplies("minimum focus size is 1",
				any((Object)1, (Object)2), minCount(1));
	}

	@Test void testType() { // !!! improve testing of multiple implications

		assertImplies("xsd:boolean has closed range",
				datatype(GAE.Boolean), and(maxCount(1), in(false, true)));

		assertImplies("xsd:boolean has exclusive values",
				datatype(GAE.Boolean), and(maxCount(1), in(false, true)));

	}

	@Test void testClazz() {
		assertImplies("classed values are resources",
				clazz("Entity"), datatype(GAE.Entity));
	}

	@Test void testRange() {

		assertImplies("maximum focus size is equal to the size of the allowed value set",
				in(1, 2.0), maxCount(2));

		assertImplies("if unique, focus values share the datatype of the allowed value set",
				in(1, 2), and(maxCount(2), datatype(GAE.Integral)));

	}

	@Test void testField() {

		assertImplies("field subjects are resources",
				field("field", and()), datatype(GAE.Entity));

		assertImplies("nested shapes are expanded",
				field("field", clazz("Class")), datatype(GAE.Entity),
				(s, i) -> and(field(s.getName(), and(and(s.getShape(), i), datatype(GAE.Entity))), datatype(GAE.Entity)));
	}

	@Test void testConjunction() {
		assertImplies("nested shapes are expanded", and(clazz("Class")), datatype(GAE.Entity),
				(s, i) -> and(Stream.concat(s.getShapes().stream(), Stream.of((Shape)i)).collect(toList()))); // outer and() stripped by optimization
	}

	@Test void testDisjunction() {
		assertImplies("nested shapes are expanded", or(clazz("Class")), datatype(GAE.Entity),
				(s, i) -> and(Stream.concat(s.getShapes().stream(), Stream.of((Shape)i)).collect(toList()))); // outer or() stripped by optimization
	}

	@Test void testOption() { // !!! uncomment when filtering constraints are accepted by when()
		assertImplies("nested shapes are expanded",
				when(and()/* !!! clazz(RDF.NIL) */, clazz("Class"), clazz("Class")),
				datatype(GAE.Entity),
				(s, i) -> when(and()/* !!! and(s.getTest(), i)*/, and(s.getPass(), i), and(s.getFail(), i))
		);
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
		return optimize(shape.map(new DatastoreInferencer()));
	}

}
