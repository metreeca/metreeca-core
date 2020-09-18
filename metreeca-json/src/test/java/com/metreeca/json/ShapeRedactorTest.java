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

import com.metreeca.json.shapes.Guard;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.guard;
import static com.metreeca.json.shapes.Guard.retain;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static org.assertj.core.api.Assertions.assertThat;


final class ShapeRedactorTest {

	private static final Function<Guard, Boolean> first=retain("value", "first");
	private static final Function<Guard, Boolean> rest=retain("value", "rest");
	private static final Function<Guard, Boolean> any=retain("value", true);
	private static final Function<Guard, Boolean> none=retain("value");

	private static final IRI X=iri("http://example.com/x");
	private static final IRI Y=iri("http://example.com/y");
	private static final IRI Z=iri("http://example.com/z");


	private Shape value(final Object... values) {
		return guard("value", values);
	}


	@Test void testIgnoreUnrelatedConditions() {

		final Shape guard=value("first");

		assertThat(guard.redact(retain("nil", "nil")))
				.as("undefined variable")
				.isEqualTo(guard);

	}

	@Test void testReplaceTargetedConditions() {

		final Shape shape=field(RDF.TYPE, and());
		final Shape guard=value("first").then(shape);

		assertThat(guard.redact(first)).as("included value").isEqualTo(shape);
		assertThat(guard.redact(rest)).as("excluded value").isEqualTo(and());
		assertThat(guard.redact(any)).as("wildcard value").isEqualTo(shape);

	}

	@Test void testRedactNestedShapes() {

		final Shape x=field(X, and());
		final Shape y=field(Y, and());
		final Shape z=field(Z, and());

		final Shape guard=value("first");

		assertThat(field(RDF.VALUE, guard.then(x)).redact(first))
				.as("field")
				.isEqualTo(field(RDF.VALUE, x));

		assertThat(and(guard.then(x), guard.then(y)).redact(first))
				.as("conjunction")
				.isEqualTo(and(x, y));

		assertThat(or(guard.then(x), guard.then(y))
				.redact(first))
				.as("disjunction")
				.isEqualTo(or(x, y));

		assertThat(when(guard.then(x), guard.then(y), guard.then(z)).redact(first))
				.as("option")
				.isEqualTo(when(x, y, z));

	}

	@Test void testHandleWildcards() {

		final Shape x=field(X, and());
		final Shape y=field(Y, and());

		assertThat(and(value("first").then(x), value("rest").then(y)).redact(any))
				.isEqualTo(and(x, y));
	}

	@Test void testHandleEMptyValueSets() {
		assertThat(value("first").redact(none))
				.isEqualTo(or());
	}


	@Test void testOptimizeFields() {
		assertThat(and(field(RDF.FIRST, value("first")), field(RDF.REST, value("rest"))).redact(first))
				.isEqualTo(field(RDF.FIRST, and()));
	}

	@Test void testOptimizeAnds() {
		assertThat(and(and(value("first"))).redact(first))
				.isEqualTo(and());
	}

}
