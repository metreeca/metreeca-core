/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.json;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.json.Focus.focus;
import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.inverse;
import static com.metreeca.json.ValuesTest.decode;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;

import static java.util.stream.Collectors.toSet;


final class ShapeOutlinerTest {

	private Collection<Statement> outline(final Shape shape, final Value... sources) {
		return shape.map(new ShapeOutliner(sources)).collect(toSet());
	}


	@Test void testOutlineFields() {

		assertThat(outline(field(RDF.VALUE).as(all(RDF.REST)), RDF.FIRST))
				.as("direct field")
				.isIsomorphicTo(decode("rdf:first rdf:value rdf:rest."));

		assertThat(outline(field(inverse(RDF.VALUE)).as(all(RDF.REST)), RDF.FIRST))
				.as("inverse field")
				.isIsomorphicTo(decode("rdf:rest rdf:value rdf:first."));

	}

	@Test void testOutlineClasses() {
		assertThat(outline(and(all(RDF.FIRST), clazz(RDFS.RESOURCE))))
				.as("classes")
				.isIsomorphicTo(decode("rdf:first a rdfs:Resource."));
	}

	@Test void testOutlineSubjectExistentials() {
		assertThat(outline(and(all(RDF.FIRST, RDF.REST), field(RDF.VALUE).as(all(RDF.NIL)))))
				.as("subject existentials")
				.isIsomorphicTo(decode("rdf:first rdf:value rdf:nil. rdf:rest rdf:value rdf:nil."));
	}

	@Test void testOutlineObjectExistentials() {
		assertThat(outline(and(all(RDF.NIL), field(RDF.VALUE).as(all(RDF.FIRST, RDF.REST)))))
				.as("object existentials")
				.isIsomorphicTo(decode("rdf:nil rdf:value rdf:first, rdf:rest."));
	}

	@Test void testOutlineConjunctions() {
		assertThat(outline(and(all(RDF.NIL), and(field(RDF.VALUE).as(all(RDF.FIRST))))))
				.as("value union")
				.isIsomorphicTo(decode("rdf:nil rdf:value rdf:first."));
	}

	@Test void testOutlineNestedConjunctions() {
		assertThat(outline(and(all(RDF.NIL), field(RDF.VALUE).as(and(all(RDF.FIRST), all(RDF.REST))))))
				.as("value union")
				.isIsomorphicTo(decode("rdf:nil rdf:value rdf:first, rdf:rest."));
	}


	@Test void testResolveReferencesToTarget() {
		assertThat(outline(field(RDF.VALUE).as(all(focus())), RDF.FIRST))
				.as("value union")
				.isIsomorphicTo(decode("rdf:first rdf:value rdf:first."));
	}

}
