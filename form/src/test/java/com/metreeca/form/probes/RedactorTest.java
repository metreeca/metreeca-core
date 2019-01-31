/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.Guard;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Guard.guard;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Field.field;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;


final class RedactorTest {

	private static final Redactor empty=new Redactor(emptyMap());

	private static final Redactor first=new Redactor(singletonMap(RDF.VALUE, singleton(RDF.FIRST)));
	private static final Redactor rest=new Redactor(singletonMap(RDF.VALUE, singleton(RDF.REST)));
	private static final Redactor any=new Redactor(singletonMap(RDF.VALUE, singleton(Form.any)));


	@Test void testIgnoreUnconditionalShapes() {

		final Shape shape=and();

		assertThat(shape.map(empty)).as("unconditional shape").isEqualTo(shape);

	}

	@Test void testIgnoreUndefinedConditions() {

		final Guard guard=guard(RDF.VALUE, RDF.FIRST);

		assertThat(guard.map(empty)).as("undefined variable").isEqualTo(guard);

	}

	@Test void testReplaceDefinedConditions() {

		final Guard guard=guard(RDF.VALUE, RDF.FIRST);

		assertThat(guard.map(first)).as("included value").isEqualTo(and());
		assertThat(guard.map(rest)).as("excluded value").isEqualTo(or());
		assertThat(guard.map(any)).as("wildcard value").isEqualTo(and());

	}

	@Test void testRedactNestedShapes() {

		final Guard nested=guard(RDF.VALUE, RDF.FIRST);

		assertThat(field(RDF.VALUE, nested).map(first)).as("field").isEqualTo(field(RDF.VALUE, and()));

		assertThat(and(nested).map(first)).as("conjunction").isEqualTo(and(and()));
		assertThat(or(nested).map(first)).as("disjunction").isEqualTo(or(and()));
		assertThat(when(and(), and(), nested).map(first)).as("option").isEqualTo(when(and(), and(), and()));

	}

}
