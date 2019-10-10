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

package com.metreeca.rdf.formats;

import com.metreeca.rdf.Values;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.metreeca.rdf.Values.inverse;
import static com.metreeca.rdf.Values.literal;
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


final class RDFInferencerTest {

	@Test void testHint() {

		assertImplies("hinted shapes are resources",
				hint("/resources/"), datatype(Values.ResourceType));

	}

	@Test void testUniversal() {
		assertImplies("minimum focus size is equal to the size of the required value set",
				all(literal(1), literal(2)), minCount(2));
	}

	@Test void testExistential() {
		assertImplies("minimum focus size is 1",
				any(literal(1), literal(2)), minCount(1));
	}

	@Test void testType() { // !!! improve testing of multiple implications

		assertImplies("xsd:boolean has closed range",
				datatype(XMLSchema.BOOLEAN), and(maxCount(1), in(literal(false), literal(true))));

		assertImplies("xsd:boolean has exclusive values",
				datatype(XMLSchema.BOOLEAN), and(maxCount(1), in(literal(false), literal(true))));

	}

	@Test void testClazz() {
		assertImplies("classed values are resources",
				clazz(RDF.NIL), datatype(Values.ResourceType));
	}

	@Test void testRange() {

		assertImplies("maximum focus size is equal to the size of the allowed value set",
				in(literal(1), literal(2.0)), maxCount(2));

		assertImplies("if unique, focus values share the datatype of the allowed value set",
				in(literal(1), literal(2)), and(maxCount(2), datatype(XMLSchema.INT)));

	}

	@Test void testField() {

		assertImplies("field subjects are resources",
				field(RDF.VALUE),
				datatype(Values.ResourceType)
		);

		assertImplies("field subjects are iris if explicitly typed",
				and(field(RDF.VALUE), datatype(Values.IRIType)),
				datatype(Values.IRIType)
		);

		assertImplies("reverse field objects are resources",
				field(inverse(RDF.VALUE)), datatype(Values.ResourceType),
				(s, i) -> field(s.getName(), and(s.getShape(), i))
		);

		assertImplies("reverse field objects are iris if explicitly typed",
				field(inverse(RDF.VALUE), datatype(Values.IRIType)),
				datatype(Values.IRIType), (s, i) -> field(s.getName(), and(s.getShape(), i))
		);

		assertImplies("both subject and object of a rdf:type field are resources",
				field(RDF.TYPE), datatype(Values.ResourceType),
				(s, i) -> and(field(s.getName(), and(s.getShape(), i)), i)
		);

		assertImplies("nested shapes are expanded",
				field(RDF.VALUE, clazz(RDF.NIL)), datatype(Values.ResourceType),
				(s, i) -> and(field(s.getName(), and(and(s.getShape(), i), datatype(Values.ResourceType))), datatype(Values.ResourceType))
		);
	}

	@Test void testConjunction() {
		assertImplies("nested shapes are expanded", and(clazz(RDF.NIL)), datatype(Values.ResourceType),
				(s, i) -> and(Stream.concat(s.getShapes().stream(), Stream.of(i)).collect(toList()))); // outer and() stripped by optimization
	}

	@Test void testDisjunction() {
		assertImplies("nested shapes are expanded", or(clazz(RDF.NIL)), datatype(Values.ResourceType),
				(s, i) -> and(Stream.concat(s.getShapes().stream(), Stream.of(i)).collect(toList()))); // outer or() stripped by optimization
	}

	@Test void testOption() { // !!! uncomment when filtering constraints are accepted by when()
		assertImplies("nested shapes are expanded",
				when(and()/* !!! clazz(RDF.NIL) */, clazz(RDF.NIL), clazz(RDF.NIL)),
				datatype(Values.ResourceType),
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
		return shape.map(new RDFOptimizer());
	}

	private Shape expand(final Shape shape) {
		return optimize(shape.map(new RDFInferencer()));
	}

}
