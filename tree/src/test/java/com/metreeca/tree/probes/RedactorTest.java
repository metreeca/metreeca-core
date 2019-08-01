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

package com.metreeca.tree.probes;

import com.metreeca.tree.Shape;
import com.metreeca.tree.shapes.Guard;

import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Guard.guard;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;


final class RedactorTest {

	private static final UnaryOperator<Shape> first=s -> s.map(new Redactor("value", "first")).map(new Optimizer());
	private static final UnaryOperator<Shape> rest=s -> s.map(new Redactor("value", "rest")).map(new Optimizer());
	private static final UnaryOperator<Shape> any=s -> s.map(new Redactor("value")).map(new Optimizer());


	@Test void testIgnoreUnrelatedConditions() {

		final Guard guard=guard("value", "first");

		assertThat(guard.map(new Redactor("nil", "nil"))).as("undefined variable").isEqualTo(guard);

	}

	@Test void testReplaceTargetedConditions() {

		final Shape shape=field("type");
		final Shape guard=guard("value", "first").then(shape);

		assertThat(guard.map(first)).as("included value").isEqualTo(shape);
		assertThat(guard.map(rest)).as("excluded value").isEqualTo(and());
		assertThat(guard.map(any)).as("wildcard value").isEqualTo(shape);

	}

	@Test void testRedactNestedShapes() {

		final Shape x=field("x");
		final Shape y=field("y");
		final Shape z=field("z");

		final Shape guard=guard("value", "first");

		assertThat(field("value", guard.then(x)).map(first))
				.as("field")
				.isEqualTo(field("value", x));

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

		final Shape x=field("x");
		final Shape y=field("y");

		assertThat(and(guard("value", "first").then(x), guard("value", "rest").then(y)).map(any))
				.as("wildcard")
				.isEqualTo(and(x, y));
	}

}
