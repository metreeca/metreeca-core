/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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
import com.metreeca.form.shapes.All;
import com.metreeca.form.things.ValuesTest;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Trait.trait;

import static org.assertj.core.api.Assertions.assertThat;


public class OutlinerTest {

	@Test public void testOutlineClasses() {
		assertThat((Object)ValuesTest.decode("rdf:first a rdfs:Resource.")).as("classes").isEqualTo(outline(and(All.all(RDF.FIRST), clazz(RDFS.RESOURCE))));
	}

	@Test public void testOutlineSubjectExistentials() {
		assertThat((Object)ValuesTest.decode("rdf:first rdf:value rdf:nil. rdf:rest rdf:value rdf:nil.")).as("subject existentials").isEqualTo(outline(and(All.all(RDF.FIRST, RDF.REST), trait(RDF.VALUE, All.all(RDF.NIL)))));
	}

	@Test public void testOutlineObjectExistentials() {
		assertThat((Object)ValuesTest.decode("rdf:nil rdf:value rdf:first, rdf:rest.")).as("object existentials").isEqualTo(outline(and(All.all(RDF.NIL), trait(RDF.VALUE, All.all(RDF.FIRST, RDF.REST)))));
	}

	@Test public void testOutlineConjunctions() {
		assertThat((Object)ValuesTest.decode("rdf:nil rdf:value rdf:first.")).as("value union").isEqualTo(outline(and(All.all(RDF.NIL), and(trait(RDF.VALUE, All.all(RDF.FIRST))))));
	}

	@Test public void testOutlineNestedConjunctions() {
		assertThat((Object)ValuesTest.decode("rdf:nil rdf:value rdf:first, rdf:rest.")).as("value union").isEqualTo(outline(and(All.all(RDF.NIL), trait(RDF.VALUE, and(All.all(RDF.FIRST), All.all(RDF.REST))))));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> outline(final Shape shape) {
		return new LinkedHashModel(shape.accept(new Outliner()));
	}

}
