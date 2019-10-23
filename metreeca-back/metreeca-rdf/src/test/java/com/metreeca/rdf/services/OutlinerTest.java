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

package com.metreeca.rdf.services;

import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.Values.inverse;
import static com.metreeca.rdf.ValuesTest.decode;
import static com.metreeca.tree.Shape.focus;
import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Field.field;

import static java.util.stream.Collectors.toSet;


final class OutlinerTest {

	@Test void testOutlineFields() {

		assertThat(outline(field(RDF.VALUE, all(RDF.REST)), RDF.FIRST))
				.as("direct field")
				.isEqualTo(decode("rdf:first rdf:value rdf:rest."));

		assertThat(outline(field(inverse(RDF.VALUE), all(RDF.REST)), RDF.FIRST))
				.as("inverse field")
				.isEqualTo(decode("rdf:rest rdf:value rdf:first."));

	}

	@Test void testOutlineClasses() {
		assertThat(outline(and(all(RDF.FIRST), clazz(RDFS.RESOURCE))))
				.as("classes")
				.isEqualTo(decode("rdf:first a rdfs:Resource."));
	}

	@Test void testOutlineSubjectExistentials() {
		assertThat(outline(and(all(RDF.FIRST, RDF.REST), field(RDF.VALUE, all(RDF.NIL)))))
				.as("subject existentials")
				.isEqualTo(decode("rdf:first rdf:value rdf:nil. rdf:rest rdf:value rdf:nil."));
	}

	@Test void testOutlineObjectExistentials() {
		assertThat(outline(and(all(RDF.NIL), field(RDF.VALUE, all(RDF.FIRST, RDF.REST)))))
				.as("object existentials")
				.isEqualTo(decode("rdf:nil rdf:value rdf:first, rdf:rest."));
	}

	@Test void testOutlineConjunctions() {
		assertThat(outline(and(all(RDF.NIL), and(field(RDF.VALUE, all(RDF.FIRST))))))
				.as("value union")
				.isEqualTo(decode("rdf:nil rdf:value rdf:first."));
	}

	@Test void testOutlineNestedConjunctions() {
		assertThat(outline(and(all(RDF.NIL), field(RDF.VALUE, and(all(RDF.FIRST), all(RDF.REST))))))
				.as("value union")
				.isEqualTo(decode("rdf:nil rdf:value rdf:first, rdf:rest."));
	}


	@Test void testResolveReferencesToTarget() {
		assertThat(outline(field(RDF.VALUE, all(focus())), RDF.FIRST))
				.as("value union")
				.isEqualTo(decode("rdf:first rdf:value rdf:first."));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> outline(final Shape shape, final Value... sources) {
		return shape.map(new Outliner(sources)).collect(toSet());
	}

}
