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

package com.metreeca.tree.probes;

import com.metreeca.tree.Shape;

import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Any.any;
import static com.metreeca.tree.shapes.In.in;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.MinCount.minCount;

import static org.assertj.core.api.Assertions.assertThat;


final class InferencerTest {

	//@Test void testHint() {
	//	assertImplies("hinted shapes are resources",
	//			hint("nil"), type(Form.ResourceType));
	//}

	@Test void testUniversal() {
		assertImplies("minimum focus size is equal to the size of the required value set",
				all(1, 2), minCount(2));
	}

	@Test void testExistential() {
		assertImplies("minimum focus size is 1",
				any(1, 2), minCount(1));
	}

	//@Test void testType() { // !!! improve testing of multiple implications
	//
	//	assertImplies("xsd:boolean has closed range",
	//			type(XMLSchema.BOOLEAN), and(maxCount(1), in(false, true)));
	//
	//	assertImplies("xsd:boolean has exclusive values",
	//			type(XMLSchema.BOOLEAN), and(maxCount(1), in(false, true)));
	//
	//}

	//@Test void testKind() {
	//	assertImplies("classed values are resources",
	//			kind("nil"), type(Form.ResourceType));
	//}

	@Test void testRange() {

		assertImplies("maximum focus size is equal to the size of the allowed value set",
				in(1, 2.0), maxCount(2)
		);

		//assertImplies("if unique, focus values share the type of the allowed value set",
		//		in(1, 2), and(maxCount(2), type(XMLSchema.INT)));

	}

	@Test void testField() {

		//assertImplies("field subjects are resources",
		//		field("value"), type(Form.ResourceType));
		//
		//assertImplies("field subjects are iris if explicitly typed",
		//		and(field("value"), type(Form.IRIType)), type(Form.IRIType));
		//
		//assertImplies("reverse field objects are resources",
		//		field(inverse("value")), type(Form.ResourceType), (s, i) -> field(s.getIRI(), and(s.getShape(), i)));
		//
		//assertImplies("reverse field objects are iris if explicitly typed",
		//		field(inverse("value"), type(Form.IRIType)), type(Form.IRIType), (s, i) -> field(s.getIRI(), and(s.getShape(), i)));
		//
		//assertImplies("both subject and object of a rdf:type field are resources",
		//		field(RDF.TYPE), type(Form.ResourceType),
		//		(s, i) -> and(field(s.getIRI(), and(s.getShape(), i)), i));

		//assertImplies("nested shapes are expanded",
		//		field("value", kind("nil")), type(Form.ResourceType),
		//		(s, i) -> and(field(s.getIRI(), and(and(s.getShape(), i), type(Form.ResourceType))), type(Form.ResourceType)));
	}

	//@Test void testConjunction() {
	//	assertImplies("nested shapes are expanded", and(kind("kind")), type("type"),
	//			(s, i) -> and(Lists.concat(s.getShapes(), list(i)))); // outer and() stripped by optimization
	//}

	//@Test void testDisjunction() {
	//	assertImplies("nested shapes are expanded", Or.or(clazz("nil")), type(Form.ResourceType),
	//			(s, i) -> and(Lists.concat(s.getShapes(), list(i)))); // outer or() stripped by optimization
	//}

	//@Test void testOption() { // !!! uncomment when filtering constraints are accepted by when()
	//	assertImplies("nested shapes are expanded",
	//			when(and()/* !!! clazz("nil") */, clazz("nil"), clazz("nil")),
	//			type(Form.ResourceType),
	//			(s, i) -> when(and()/* !!! and(s.getTest(), i)*/, and(s.getPass(), i), and(s.getFail(), i))
	//	);
	//}


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
