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

package com.metreeca.json.probes;

import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.guard;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static org.assertj.core.api.Assertions.assertThat;


final class RedactorTest {

	private static final UnaryOperator<Shape> first=s -> s.map(new Redactor("value", "first"));
	private static final UnaryOperator<Shape> rest=s -> s.map(new Redactor("value", "rest"));
	private static final UnaryOperator<Shape> any=s -> s.map(new Redactor("value", values -> true));

	private static final IRI X=iri("http://example.com/x");
	private static final IRI Y=iri("http://example.com/y");
	private static final IRI Z=iri("http://example.com/z");


	private Shape value(final Object... values) {
		return guard("value", values);
	}


	@Test void testIgnoreUnrelatedConditions() {

		final Shape guard=value("first");

		assertThat(guard.map(new Redactor("nil", "nil"))).as("undefined variable").isEqualTo(guard);

	}

	@Test void testReplaceTargetedConditions() {

		final Shape shape=field(RDF.TYPE, and());
		final Shape guard=value("first").then(shape);

		assertThat(guard.map(first)).as("included value").isEqualTo(shape);
		assertThat(guard.map(rest)).as("excluded value").isEqualTo(and());
		assertThat(guard.map(any)).as("wildcard value").isEqualTo(shape);

	}

	@Test void testRedactNestedShapes() {

		final Shape x=field(X, and());
		final Shape y=field(Y, and());
		final Shape z=field(Z, and());

		final Shape guard=value("first");

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

		final Shape x=field(X, and());
		final Shape y=field(Y, and());

		assertThat(and(value("first").then(x), value("rest").then(y)).map(any))
				.as("wildcard")
				.isEqualTo(and(x, y));
	}


	@Test void testOptimizeFields() {
		assertThat(and(field(RDF.FIRST, value("first")), field(RDF.REST, value("rest"))).map(first))
				.isEqualTo(field(RDF.FIRST, and()));
	}

	@Test void testOptimizeAnds() {
		assertThat(and(and(value("first"))).map(first))
				.isEqualTo(and());
	}

}
