/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.json;

import com.metreeca.json.shapes.Range;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static org.assertj.core.api.Assertions.assertThat;


final class ShapeInferencerTest {

	private Shape expand(final Shape shape) {
		return shape.map(new ShapeInferencer());
	}


	@Test void testAll() {

		final Shape all=all(literal(1), literal(2));

		assertThat(expand(all))
				.as("minimum focus size is equal to the size of the required value set")
				.isEqualTo(and(all, minCount(2)));
	}

	@Test void testAny() {

		final Shape any=any(literal(1), literal(2));

		assertThat(expand(any))
				.as("minimum focus size is 1")
				.isEqualTo(and(any, minCount(1)));
	}

	@Test void testDatatype() {

		final Shape datatype=datatype(XSD.BOOLEAN);

		assertThat(expand(datatype))
				.as("xsd:boolean has exclusive values and closed range")
				.isEqualTo(and(datatype, and(maxCount(1), Range.range(literal(false), literal(true)))));
	}

	@Test void testClazz() {

		final Shape clazz=clazz(RDF.NIL);

		assertThat(expand(clazz))
				.as("classed values are resources")
				.isEqualTo(and(clazz, datatype(ResourceType)));
	}

	@Test void testIn() {

		final Shape Eterogeneous=Range.range(literal(1), literal(2.0));

		assertThat(expand(Eterogeneous))
				.as("maximum focus size is equal to the size of the allowed value set")
				.isEqualTo(and(Eterogeneous, maxCount(2)));

		final Shape inUniform=Range.range(literal(1), literal(2));

		assertThat(expand(inUniform))
				.as("if unique, focus values share the datatype of the allowed value set")
				.isEqualTo(and(inUniform, and(maxCount(2), datatype(XSD.INT))));

	}

	@Test void testField() {

		final Shape nested=clazz(RDF.NIL);

		assertThat(expand(field(RDF.VALUE, nested)))
				.as("nested shapes are expanded")
				.isEqualTo(and(
						datatype(ResourceType),
						field(RDF.VALUE, and(nested, datatype(ResourceType)))
				));

		assertThat(expand(field(RDF.TYPE)))
				.as("rdf:type field have resource subjects and IRI objects")
				.isEqualTo(and(
						datatype(ResourceType),
						and(field(RDF.TYPE, datatype(ResourceType)), datatype(IRIType))
				));

	}

	@Test void testFieldDirect() {

		final Shape plain=field(RDF.VALUE);

		assertThat(expand(plain))
				.as("field subjects are resources")
				.isEqualTo(and(plain, datatype(ResourceType)));

		final Shape typed=and(field(RDF.VALUE), datatype(IRIType));

		assertThat(expand(typed))
				.as("field subjects are IRIs if explicitly typed")
				.isEqualTo(and(typed, and(plain, datatype(IRIType))));

	}

	@Test void testFieldInverse() {

		final Shape plain=field(inverse(RDF.VALUE));

		assertThat(expand(plain))
				.as("reverse field objects are resources")
				.isEqualTo(field(inverse(RDF.VALUE), datatype(ResourceType)));

		final Shape typed=field(inverse(RDF.VALUE), datatype(IRIType));

		assertThat(expand(typed))
				.as("reverse field objects are IRIs if explicitly typed")
				.isEqualTo(field(inverse(RDF.VALUE), datatype(IRIType)));

	}

	@Test void testAnd() {

		final Shape and=and(clazz(RDF.FIRST), clazz(RDF.REST));

		assertThat(expand(and))
				.as("nested shapes are expanded")
				.isEqualTo(and(
						and(clazz(RDF.FIRST), datatype(ResourceType)),
						and(clazz(RDF.REST), datatype(ResourceType))
				));
	}

	@Test void testOr() {

		final Shape or=or(clazz(RDF.FIRST), clazz(RDF.REST));

		assertThat(expand(or))
				.as("nested shapes are expanded")
				.isEqualTo(or(
						and(clazz(RDF.FIRST), datatype(ResourceType)),
						and(clazz(RDF.REST), datatype(ResourceType))
				));
	}

	@Test void testWhen() {

		final Shape when=when(clazz(RDF.NIL), clazz(RDF.FIRST), clazz(RDF.REST));

		assertThat(expand(when))
				.as("nested shapes are expanded")
				.isEqualTo(when(
						and(clazz(RDF.NIL), datatype(ResourceType)),
						and(clazz(RDF.FIRST), datatype(ResourceType)),
						and(clazz(RDF.REST), datatype(ResourceType))
				));
	}

}
