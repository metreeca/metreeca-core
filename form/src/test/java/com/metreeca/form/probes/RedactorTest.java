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

package com.metreeca.form.probes;

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.Guard;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Guard.guard;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.ValuesTest.term;

import static org.assertj.core.api.Assertions.assertThat;


final class RedactorTest {

	private static final UnaryOperator<Shape> first=s -> s.map(new Redactor(RDF.VALUE, RDF.FIRST)).map(new Optimizer());
	private static final UnaryOperator<Shape> rest=s -> s.map(new Redactor(RDF.VALUE, RDF.REST)).map(new Optimizer());
	private static final UnaryOperator<Shape> any=s -> s.map(new Redactor(RDF.VALUE)).map(new Optimizer());


	@Test void testIgnoreUnrelatedConditions() {

		final Guard guard=guard(RDF.VALUE, RDF.FIRST);

		assertThat(guard.map(new Redactor(RDF.NIL, RDF.NIL))).as("undefined variable").isEqualTo(guard);

	}

	@Test void testReplaceTargetedConditions() {

		final Shape shape=field(RDF.TYPE);
		final Shape guard=guard(RDF.VALUE, RDF.FIRST).then(shape);

		assertThat(guard.map(first)).as("included value").isEqualTo(shape);
		assertThat(guard.map(rest)).as("excluded value").isEqualTo(pass());
		assertThat(guard.map(any)).as("wildcard value").isEqualTo(shape);

	}

	@Test void testRedactNestedShapes() {

		final Shape x=field(term("x"));
		final Shape y=field(term("y"));
		final Shape z=field(term("z"));

		final Shape guard=guard(RDF.VALUE, RDF.FIRST);

		assertThat(field(RDF.VALUE, guard.then(x)).map(first))
				.as("field")
				.isEqualTo(field(RDF.VALUE, x));

		assertThat(and(guard.then(x), guard.then(y)).map(first))
				.as("conjunction")
				.isEqualTo(and(x, y));

		assertThat(or(guard.then(x), guard.then(y))
				.map(first)).as("disjunction")
				.isEqualTo(or(x, y));

		assertThat(when(guard.then(x), guard.then(y), guard.then(z)).map(first))
				.as("option")
				.isEqualTo(when(x, y, z));

	}

	@Test void testHandleWildcards() {

		final Shape x=field(term("x"));
		final Shape y=field(term("y"));

		assertThat(and(guard(RDF.VALUE, RDF.FIRST).then(x), guard(RDF.VALUE, RDF.REST).then(y)).map(any))
				.as("wildcard")
				.isEqualTo(and(x, y));
	}

}
