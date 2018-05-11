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

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.Test;

import java.util.Collection;

import static com.metreeca.spec.ValuesTest.parse;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Clazz.clazz;
import static com.metreeca.spec.shapes.Trait.trait;

import static org.junit.Assert.assertEquals;


public class OutlinerTest {

	@Test public void testOutlineClasses() {
		assertEquals("classes",
				parse("rdf:first a rdfs:Resource."),
				outline(and(all(RDF.FIRST), clazz(RDFS.RESOURCE))));
	}

	@Test public void testOutlineSubjectExistentials() {
		assertEquals("subject existentials",
				parse("rdf:first rdf:value rdf:nil. rdf:rest rdf:value rdf:nil."),
				outline(and(all(RDF.FIRST, RDF.REST), trait(RDF.VALUE, all(RDF.NIL)))));
	}

	@Test public void testOutlineObjectExistentials() {
		assertEquals("object existentials",
				parse("rdf:nil rdf:value rdf:first, rdf:rest."),
				outline(and(all(RDF.NIL), trait(RDF.VALUE, all(RDF.FIRST, RDF.REST)))));
	}

	@Test public void testOutlineConjunctions() {
		assertEquals("value union",
				parse("rdf:nil rdf:value rdf:first."),
				outline(and(all(RDF.NIL), and(trait(RDF.VALUE, all(RDF.FIRST))))));
	}

	@Test public void testOutlineNestedConjunctions() {
		assertEquals("value union",
				parse("rdf:nil rdf:value rdf:first, rdf:rest."),
				outline(and(all(RDF.NIL), trait(RDF.VALUE, and(all(RDF.FIRST), all(RDF.REST))))));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> outline(final Shape shape) {
		return new LinkedHashModel(shape.accept(new Outliner()));
	}

}
