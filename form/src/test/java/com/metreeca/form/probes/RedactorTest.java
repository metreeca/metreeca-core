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

import com.metreeca.form.Form;
import com.metreeca.form.shapes.When;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.Shape;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Test;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Test.test;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.shapes.Virtual.virtual;
import static com.metreeca.form.shapes.When.when;

import static org.junit.Assert.assertEquals;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;


public class RedactorTest {

	private static final Redactor empty=new Redactor(emptyMap());

	private static final Redactor first=new Redactor(singletonMap(RDF.VALUE, singleton(RDF.FIRST)));
	private static final Redactor rest=new Redactor(singletonMap(RDF.VALUE, singleton(RDF.REST)));
	private static final Redactor any=new Redactor(singletonMap(RDF.VALUE, singleton(Form.any)));


	@Test public void testIgnoreUnconditionalShapes() {

		final Shape shape=and();

		assertEquals("unconditional shape", shape, shape.accept(empty));

	}

	@Test public void testIgnoreUndefinedConditions() {

		final When when=When.when(RDF.VALUE, RDF.FIRST);

		assertEquals("undefined variable", when, when.accept(empty));

	}

	@Test public void testReplaceDefinedConditions() {

		final When when=When.when(RDF.VALUE, RDF.FIRST);

		assertEquals("included value", and(), when.accept(first));
		assertEquals("excluded value", or(), when.accept(rest));
		assertEquals("wildcard value", and(), when.accept(any));

	}

	@Test public void testRedactNestedShapes() {

		final When nested=When.when(RDF.VALUE, RDF.FIRST);

		assertEquals("trait",
				trait(RDF.VALUE, and()),
				trait(RDF.VALUE, nested).accept(first));

		assertEquals("virtual",
				virtual(trait(RDF.VALUE, and()), Step.step(RDF.NIL)),
				virtual(trait(RDF.VALUE, nested), Step.step(RDF.NIL)).accept(first));

		assertEquals("conjunction", and(and()), and(nested).accept(first));
		assertEquals("disjunction", or(and()), or(nested).accept(first));
		Assert.assertEquals("option", com.metreeca.form.shapes.Test.test(and(), and(), and()), com.metreeca.form.shapes.Test.test(and(), and(), nested).accept(first));

	}

}
