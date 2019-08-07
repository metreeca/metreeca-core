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

package com.metreeca.gae.services;

import com.metreeca.gae.GAE;
import com.metreeca.gae.GAETest;
import com.metreeca.tree.Shape;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PropertyContainer;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.function.Consumer;

import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Any.any;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.In.in;
import static com.metreeca.tree.shapes.Like.like;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.tree.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.tree.shapes.MaxLength.maxLength;
import static com.metreeca.tree.shapes.MinCount.minCount;
import static com.metreeca.tree.shapes.MinExclusive.minExclusive;
import static com.metreeca.tree.shapes.MinInclusive.minInclusive;
import static com.metreeca.tree.shapes.MinLength.minLength;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.Pattern.pattern;
import static com.metreeca.tree.shapes.Type.type;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;


final class DatastoreValidatorTest extends GAETest {

	private boolean validate(final Shape shape, final Object value) {

		final Entity entity=new Entity("*", "*");

		entity.setProperty("value", value);

		return validate(shape, entity);
	}

	private boolean validate(final Shape shape, final Object... values) {

		final Entity entity=new Entity("*", "*");

		entity.setProperty("value", asList(values));

		return validate(shape, entity);
	}

	private boolean validate(final Shape shape, final Entity entity) {
		return new DatastoreValidator().validate(field("value", shape), entity).isEmpty();
	}


	private PropertyContainer entity(final Consumer<PropertyContainer> builder) {

		final EmbeddedEntity entity=new EmbeddedEntity();

		builder.accept(entity);

		return entity;
	}


	@Test void testValidateType() {
		exec(() -> {

			assertThat(validate(type(GAE.Entity), entity(entity -> {}))).isTrue();
			assertThat(validate(type(GAE.Entity), 1)).isFalse();

			assertThat(validate(type(GAE.Boolean), true)).isTrue();
			assertThat(validate(type(GAE.Boolean), 1)).isFalse();

			assertThat(validate(type(GAE.Integer), 1L)).isTrue();
			assertThat(validate(type(GAE.Integer), 1)).isTrue();
			assertThat(validate(type(GAE.Integer), "")).isFalse();

			assertThat(validate(type(GAE.Floating), 1.0D)).isTrue();
			assertThat(validate(type(GAE.Floating), 1.0F)).isTrue();
			assertThat(validate(type(GAE.Floating), "")).isFalse();

			assertThat(validate(type(GAE.String), "text")).isTrue();
			assertThat(validate(type(GAE.String), 1)).isFalse();

			assertThat(validate(type(GAE.Date), new Date())).isTrue();
			assertThat(validate(type(GAE.Date), 1)).isFalse();

			assertThat(validate(type("*"))).as("empty focus").isTrue();

		});
	}

	@Test void testValidateClazz() {
		//exec(() -> {
		//
		//	final Shape shape=clazz("Employee");
		//
		//	// validate using type info retrieved from model
		//
		//	assertThat(validate(shape, "<employees/9999>", "<employees/9999> a :Employee")).isTrue();
		//	assertThat(validate(shape, "<offices/9999>")).isFalse();
		//
		//	// validate using type info retrieved from graph
		//
		//	assertThat(validate(shape, "<employees/1370>")).isTrue();
		//	assertThat(validate(shape, "<offices/1>")).isFalse();
		//
		//});
	}


	@Test void testValidateMinExclusive() {
		exec(() -> {

			final Shape shape=minExclusive(1);

			assertThat(validate(shape, 2)).isTrue();
			assertThat(validate(shape, 1)).isFalse();
			assertThat(validate(shape, 0)).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateMaxExclusive() {
		exec(() -> {

			final Shape shape=maxExclusive(10);

			assertThat(validate(shape, 2)).isTrue();
			assertThat(validate(shape, 10)).isFalse();
			assertThat(validate(shape, 100)).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateMinInclusive() {
		exec(() -> {

			final Shape shape=minInclusive(1);

			assertThat(validate(shape, 2)).isTrue();
			assertThat(validate(shape, 1)).isTrue();
			assertThat(validate(shape, 0)).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateMaxInclusive() {
		exec(() -> {

			final Shape shape=maxInclusive(10);

			assertThat(validate(shape, 2)).isTrue();
			assertThat(validate(shape, 10)).isTrue();
			assertThat(validate(shape, 100)).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}


	@Test void testValidateMinLength() {
		exec(() -> {

			final Shape shape=minLength(3);

			assertThat(validate(shape, 100)).isTrue();
			assertThat(validate(shape, 99)).isFalse();

			assertThat(validate(shape, "100")).isTrue();
			assertThat(validate(shape, "99")).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateMaxLength() {
		exec(() -> {

			final Shape shape=maxLength(2);

			assertThat(validate(shape, 99)).isTrue();
			assertThat(validate(shape, 100)).isFalse();

			assertThat(validate(shape, "99")).isTrue();
			assertThat(validate(shape, "100")).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidatePattern() {
		exec(() -> {

			final Shape shape=pattern(".*\\.org");

			assertThat(validate(shape, "example.org")).isTrue();
			assertThat(validate(shape, "example.com")).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateLike() {
		exec(() -> {

			final Shape shape=like("ex.org");

			assertThat(validate(shape, "<http://exampe.org/>")).isTrue();
			assertThat(validate(shape, "<http://exampe.com/>")).isFalse();

			assertThat(validate(shape, "'example.org'")).isTrue();
			assertThat(validate(shape, "'example.com'")).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}


	@Test void testValidateMinCount() {
		exec(() -> {

			final Shape shape=minCount(2);

			assertThat(validate(shape, 1, 2, 3)).isTrue();
			assertThat(validate(shape, 1)).isFalse();

		});
	}

	@Test void testValidateMaxCount() {
		exec(() -> {

			final Shape shape=maxCount(2);

			assertThat(validate(shape, 1, 2)).isTrue();
			assertThat(validate(shape, 1, 2, 3)).isFalse();

		});
	}

	@Test void testValidateMaxCountImpliedSingleton() {
		exec(() -> {

			final Shape shape=maxCount(1);

			assertThat(validate(shape, 1)).as("single value").isTrue();
			assertThat(validate(shape, new Object[] {1})).as("singleton list").isFalse();

		});
	}

	@Test void testValidateIn() {
		exec(() -> {

			final Shape shape=in(1, 2);

			assertThat(validate(shape, 1)).isTrue();
			assertThat(validate(shape, 1, 2)).isTrue();
			assertThat(validate(shape, 1, 2, 3)).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateAll() {
		exec(() -> {

			final Shape shape=all(1, 2);

			assertThat(validate(shape, 1)).isFalse();
			assertThat(validate(shape, 1, 2)).isTrue();
			assertThat(validate(shape, 1, 2, 3)).isTrue();

			assertThat(validate(shape)).as("empty focus").isFalse();

		});
	}

	@Test void testValidateAny() {
		exec(() -> {

			final Shape shape=any(1, 2);

			assertThat(validate(shape, 1)).isTrue();
			assertThat(validate(shape, 3)).isFalse();

			assertThat(validate(shape)).as("empty focus").isFalse();

		});
	}


	@Test void testValidateField() {
		exec(() -> {

			final Shape shape=field("field", maxCount(2));

			assertThat(validate(shape, entity(entity -> entity.setProperty("field", 1)))).isTrue();
			assertThat(validate(shape, entity(entity -> entity.setProperty("field", asList(1, 2))))).isTrue();
			assertThat(validate(shape, entity(entity -> entity.setProperty("field", asList(1, 2, 3))))).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateFieldImpliedEntity() {
		exec(() -> {

			final Shape shape=field("field", required());

			assertThat(validate(shape, 1))
					.as("not an entity")
					.isFalse();

			assertThat(validate(shape, entity(entity -> {})))
					.as("entity with missing fields")
					.isFalse();

			assertThat(validate(shape, entity(entity -> entity.setProperty("unknown", ""))))
					.as("entity with unknown fields")
					.isFalse();

			assertThat(validate(shape, entity(entity -> entity.setProperty("field", ""))))
					.as("valid entity")
					.isTrue();

		});
	}


	@Test void testValidateAnd() {
		exec(() -> {

			final Shape shape=and(any(1), any(2));

			assertThat(validate(shape, 1, 2, 3)).isTrue();
			assertThat(validate(shape, 1, 3)).isFalse();

			assertThat(validate(shape)).as("empty focus").isFalse();

		});
	}

	@Test void testValidateOr() {
		exec(() -> {

			final Shape shape=or(all(2, 3), all(3, 4));

			assertThat(validate(shape, 1, 2, 3)).isTrue();
			assertThat(validate(shape, 1, 2)).isFalse();

		});
	}

}
