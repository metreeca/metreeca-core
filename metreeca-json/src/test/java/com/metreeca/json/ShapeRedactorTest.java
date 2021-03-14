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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.json.shapes.Link.link;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.emptySet;


final class ShapeRedactorTest {

	private static final IRI X=iri("test:x");
	private static final IRI Y=iri("test:y");
	private static final IRI Z=iri("test:z");


	private Shape value(final Object... values) {
		return guard("value", values);
	}


	@Test void testIgnoreUnrelatedConditions() {

		final Shape guard=value("first");

		assertThat(guard.redact("nil", "nil"))
				.as("undefined variable")
				.isEqualTo(guard);

	}

	@Test void testReplaceTargetedConditions() {

		final Shape shape=field(RDF.TYPE);
		final Shape guard=value("first").then(shape);

		assertThat(guard.redact("value", "first")).as("included value").isEqualTo(shape);
		assertThat(guard.redact("value", "rest")).as("excluded value").isEqualTo(and());
		assertThat(guard.redact("value")).as("wildcard value").isEqualTo(shape);

	}

	@Test void testRedactNestedShapes() {

		final Shape x=field(X);
		final Shape y=field(Y);
		final Shape z=field(Z);

		final Shape guard=value("first");

		assertThat(field(RDF.VALUE, guard.then(x)).redact("value", "first"))
				.as("field")
				.isEqualTo(field(RDF.VALUE, x));

		assertThat(and(guard.then(x), guard.then(y)).redact("value", "first"))
				.as("conjunction")
				.isEqualTo(and(x, y));

		assertThat(or(guard.then(x), guard.then(y))
				.redact("value", "first"))
				.as("disjunction")
				.isEqualTo(or(x, y));

		assertThat(when(guard.then(x), guard.then(y), guard.then(z)).redact("value", "first"))
				.as("option")
				.isEqualTo(when(x, y, z));

	}

	@Test void testHandleWildcards() {

		final Shape x=field(X, and());
		final Shape y=field(Y, and());

		assertThat(and(value("first").then(x), value("rest").then(y)).redact("value"))
				.isEqualTo(and(x, y));
	}

	@Test void testHandleEmptyValueSets() {
		assertThat(value("first").redact("value", emptySet()))
				.isEqualTo(or());
	}


	@Test void testOptimizeFields() {
		assertThat(and(field(RDF.FIRST, value("first")), field(RDF.REST, value("rest")))
				.redact("value", "first"))
				.isEqualTo(field(RDF.FIRST, and()));
	}

	@Test void testLimitLinksToRelateTask() {

		final Shape link=link(OWL.SAMEAS, field(RDF.NIL));

		assertThat(link.redact(Task, Relate)).as("retain on relate task").isEqualTo(link);
		assertThat(link.redact(Task, Update)).as("redact on other tasks").isEqualTo(and());

		assertThat(link.redact(View, Detail)).as("immaterial axis").isEqualTo(link);

	}

	@Test void testOptimizeAnds() {
		assertThat(and(and(value("first"))).redact("value", "first"))
				.isEqualTo(and());
	}

}
