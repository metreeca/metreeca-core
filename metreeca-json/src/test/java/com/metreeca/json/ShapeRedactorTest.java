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

		final Shape shape=field(RDF.TYPE);
		final Shape guard=value("first").then(shape);

		assertThat(guard.redact(first)).as("included value").isEqualTo(shape);
		assertThat(guard.redact(rest)).as("excluded value").isEqualTo(and());
		assertThat(guard.redact(any)).as("wildcard value").isEqualTo(shape);

	}

	@Test void testRedactNestedShapes() {

		final Shape x=field(X);
		final Shape y=field(Y);
		final Shape z=field(Z);

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
