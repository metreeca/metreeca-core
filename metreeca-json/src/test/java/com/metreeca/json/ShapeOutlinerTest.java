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

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.json.Focus.focus;
import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;

import static java.util.stream.Collectors.toSet;


final class ShapeOutlinerTest {

	private static final IRI p=iri("test:p");
	private static final IRI q=iri("test:q");

	private static final IRI x=iri("test:x");
	private static final IRI y=iri("test:y");
	private static final IRI z=iri("test:z");


	private Collection<Statement> outline(final Shape shape, final Value... sources) {
		return shape.map(new ShapeOutliner(sources)).collect(toSet());
	}

	@Test void testOutlineFields() {

		assertThat(outline(field(p, all(y)), x))
				.as("direct field")
				.isIsomorphicTo(statement(x, p, y));

		assertThat(outline(field(inverse(p), all(y)), x))
				.as("inverse field")
				.isIsomorphicTo(statement(y, p, x));

	}


	@Test void testOutlineClasses() {
		assertThat(outline(and(all(x), clazz(y))))
				.as("classes")
				.isIsomorphicTo(statement(x, RDF.TYPE, y));
	}

	@Test void testOutlineSubjectExistentials() {
		assertThat(outline(and(all(x, y), field(p, all(z)))))
				.as("subject existentials")
				.isIsomorphicTo(statement(x, p, z), statement(y, p, z));
	}

	@Test void testOutlineObjectExistentials() {
		assertThat(outline(and(all(z), field(p, all(x, y)))))
				.as("object existentials")
				.isIsomorphicTo(statement(z, p, x), statement(z, p, y));
	}

	@Test void testOutlineConjunctions() {
		assertThat(outline(and(all(z), and(field(p, all(x))))))
				.as("value union")
				.isIsomorphicTo(statement(z, p, x));
	}

	@Test void testOutlineNestedConjunctions() {
		assertThat(outline(and(all(z), field(p, and(all(x), all(y))))))
				.as("value union")
				.isIsomorphicTo(statement(z, p, x), statement(z, p, y));
	}


	@Test void testResolveReferencesToTarget() {
		assertThat(outline(field(p, all(focus())), x))
				.as("value union")
				.isIsomorphicTo(statement(x, p, x));
	}

}
