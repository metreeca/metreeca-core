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

package com.metreeca.gcp.services;

import com.metreeca.gcp.GCP;
import com.metreeca.gcp.formats.EntityFormat;
import com.metreeca.rest.Request;
import com.metreeca.tree.Shape;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Any.any;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
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

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;


final class DatastoreValidatorTest extends DatastoreTestBase {

	private boolean validate(final Shape shape, final Value<?> value) {
		return validate(shape, Entity
				.newBuilder(key(GCP.Resource, "/entities/test"))
				.set("value", value)
				.build()
		);
	}

	private boolean validate(final Shape shape, final Value<?>... values) {
		return validate(shape, Entity.newBuilder(key(GCP.Resource, "/entities/test"))
				.set("value", asList(values))
				.build()
		);
	}

	private boolean validate(final Shape shape, final Entity entity) {
		return new DatastoreValidator(service(datastore()))
				.validate(new Request()
						.shape(field("value", shape))
						.body(EntityFormat.entity(), entity)
				)
				.value()
				.isPresent();
	}


	private Key key(final String type, final String id) {
		return service(datastore()).newKeyFactory().setKind(type).newKey(id);
	}

	private FullEntity<?> entity(final Consumer<FullEntity.Builder<IncompleteKey>> task) {

		final FullEntity.Builder<IncompleteKey> builder=FullEntity.newBuilder();

		task.accept(builder);

		return builder.build();
	}


	@Test void testValidateDatatype() {
		exec(() -> {

			assertThat(validate(datatype(ValueType.ENTITY), EntityValue.of(entity(entity -> {})))).isTrue();
			assertThat(validate(datatype(ValueType.ENTITY), LongValue.of(1))).isFalse();

			assertThat(validate(datatype(ValueType.BOOLEAN), BooleanValue.of(true))).isTrue();
			assertThat(validate(datatype(ValueType.BOOLEAN), LongValue.of(1))).isFalse();

			assertThat(validate(datatype(ValueType.LONG), LongValue.of(1))).isTrue();
			assertThat(validate(datatype(ValueType.LONG), StringValue.of(""))).isFalse();

			assertThat(validate(datatype(ValueType.DOUBLE), DoubleValue.of(1))).isTrue();
			assertThat(validate(datatype(ValueType.DOUBLE), StringValue.of(""))).isFalse();

			assertThat(validate(datatype(ValueType.STRING), StringValue.of("text"))).isTrue();
			assertThat(validate(datatype(ValueType.STRING), LongValue.of(1))).isFalse();

			assertThat(validate(datatype(ValueType.TIMESTAMP), TimestampValue.of(Timestamp.now()))).isTrue();
			assertThat(validate(datatype(ValueType.TIMESTAMP), LongValue.of(1))).isFalse();

			assertThat(validate(datatype("Any"))).as("empty focus").isTrue();

		});
	}

	@Test void testValidateClazz() {
		exec(() -> {

			final Shape shape=clazz("Class");

			assertThat(validate(shape, EntityValue.of(entity(entity -> entity.setKey(key("Class", "/id")))))).isTrue();
			assertThat(validate(shape, EntityValue.of(entity(entity -> entity.setKey(key("Other", "/id")))))).isFalse();
			assertThat(validate(shape, LongValue.of(1))).isFalse();

		});
	}


	@Test void testValidateMinExclusive() {
		exec(() -> {

			final Shape shape=minExclusive(1);

			assertThat(validate(shape, LongValue.of(2))).isTrue();
			assertThat(validate(shape, LongValue.of(1))).isFalse();
			assertThat(validate(shape, LongValue.of(0))).isFalse();

			assertThat(validate(shape, StringValue.of(""))).isTrue();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateMaxExclusive() {
		exec(() -> {

			final Shape shape=maxExclusive(10);

			assertThat(validate(shape, LongValue.of(2))).isTrue();
			assertThat(validate(shape, LongValue.of(10))).isFalse();
			assertThat(validate(shape, LongValue.of(100))).isFalse();

			assertThat(validate(shape, StringValue.of(""))).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateMinInclusive() {
		exec(() -> {

			final Shape shape=minInclusive(1);

			assertThat(validate(shape, LongValue.of(2))).isTrue();
			assertThat(validate(shape, LongValue.of(1))).isTrue();
			assertThat(validate(shape, LongValue.of(0))).isFalse();

			assertThat(validate(shape, StringValue.of(""))).isTrue();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateMaxInclusive() {
		exec(() -> {

			final Shape shape=maxInclusive(10);

			assertThat(validate(shape, LongValue.of(2))).isTrue();
			assertThat(validate(shape, LongValue.of(10))).isTrue();
			assertThat(validate(shape, LongValue.of(100))).isFalse();

			assertThat(validate(shape, StringValue.of(""))).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}


	@Test void testValidateMinLength() {
		exec(() -> {

			final Shape shape=minLength(3);

			assertThat(validate(shape, LongValue.of(100))).isTrue();
			assertThat(validate(shape, LongValue.of(99))).isFalse();

			assertThat(validate(shape, StringValue.of("100"))).isTrue();
			assertThat(validate(shape, StringValue.of("99"))).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateMaxLength() {
		exec(() -> {

			final Shape shape=maxLength(2);

			assertThat(validate(shape, LongValue.of(99))).isTrue();
			assertThat(validate(shape, LongValue.of(100))).isFalse();

			assertThat(validate(shape, StringValue.of("99"))).isTrue();
			assertThat(validate(shape, StringValue.of("100"))).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidatePattern() {
		exec(() -> {

			final Shape shape=pattern(".*\\.org");

			assertThat(validate(shape, StringValue.of("example.org"))).isTrue();
			assertThat(validate(shape, StringValue.of("example.com"))).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateLike() {
		exec(() -> {

			final Shape shape=like("ex.org");

			assertThat(validate(shape, StringValue.of("<http://exampe.org/>"))).isTrue();
			assertThat(validate(shape, StringValue.of("<http://exampe.com/>"))).isFalse();

			assertThat(validate(shape, StringValue.of("'example.org'"))).isTrue();
			assertThat(validate(shape, StringValue.of("'example.com'"))).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}


	@Test void testValidateMinCount() {
		exec(() -> {

			final Shape shape=minCount(2);

			assertThat(validate(shape, LongValue.of(1), LongValue.of(2), LongValue.of(3))).isTrue();
			assertThat(validate(shape, LongValue.of(1))).isFalse();

		});
	}

	@Test void testValidateMaxCount() {
		exec(() -> {

			final Shape shape=maxCount(2);

			assertThat(validate(shape, LongValue.of(1), LongValue.of(2))).isTrue();
			assertThat(validate(shape, LongValue.of(1), LongValue.of(2), LongValue.of(3))).isFalse();

		});
	}

	@Test void testValidateMaxCountImpliedSingleton() {
		exec(() -> {

			final Shape shape=maxCount(1);

			assertThat(validate(shape, LongValue.of(1))).as("single value").isTrue();
			assertThat(validate(shape, new Value[] {LongValue.of(1)})).as("singleton list").isFalse();

		});
	}


	@Test void testValidateIn() {
		exec(() -> {

			final Shape shape=in(1, 2);

			assertThat(validate(shape, LongValue.of(1))).isTrue();
			assertThat(validate(shape, LongValue.of(1), LongValue.of(2))).isTrue();
			assertThat(validate(shape, LongValue.of(1), LongValue.of(2), LongValue.of(3))).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateAll() {
		exec(() -> {

			final Shape shape=all(1, 2);

			assertThat(validate(shape, LongValue.of(1))).isFalse();
			assertThat(validate(shape, LongValue.of(1), LongValue.of(2))).isTrue();
			assertThat(validate(shape, LongValue.of(1), LongValue.of(2), LongValue.of(3))).isTrue();

			assertThat(validate(shape)).as("empty focus").isFalse();

		});
	}

	@Test void testValidateAny() {
		exec(() -> {

			final Shape shape=any(1, 2);

			assertThat(validate(shape, LongValue.of(1))).isTrue();
			assertThat(validate(shape, LongValue.of(3))).isFalse();

			assertThat(validate(shape)).as("empty focus").isFalse();

		});
	}


	@Test void testValidateField() {
		exec(() -> {

			final Shape shape=field("field", maxCount(2));

			assertThat(validate(shape, EntityValue.of(entity(entity -> entity.set("field", 1))))).isTrue();
			assertThat(validate(shape, EntityValue.of(entity(entity -> entity.set("field", 1, 2))))).isTrue();
			assertThat(validate(shape, EntityValue.of(entity(entity -> entity.set("field", 1, 2, 3))))).isFalse();

			assertThat(validate(shape)).as("empty focus").isTrue();

		});
	}

	@Test void testValidateFieldImpliedEntity() {
		exec(() -> {

			final Shape shape=field("field", required());

			assertThat(validate(shape, LongValue.of(1)))
					.as("not an entity")
					.isFalse();

			assertThat(validate(shape, EntityValue.of(entity(entity -> {}))))
					.as("entity with missing fields")
					.isFalse();

			assertThat(validate(shape, EntityValue.of(entity(entity -> entity.set("unknown", "")))))
					.as("entity with unknown fields")
					.isFalse();

			assertThat(validate(shape, EntityValue.of(entity(entity -> entity.set("field", "")))))
					.as("valid entity")
					.isTrue();

		});
	}

	@Test void testValidateAnd() {
		exec(() -> {

			final Shape shape=and(any(1), any(2));

			assertThat(validate(shape, LongValue.of(1), LongValue.of(2), LongValue.of(3))).isTrue();
			assertThat(validate(shape, LongValue.of(1), LongValue.of(3))).isFalse();

			assertThat(validate(shape)).as("empty focus").isFalse();

		});
	}

	@Test void testValidateOr() {
		exec(() -> {

			final Shape shape=or(all(2, 3), all(3, 4));

			assertThat(validate(shape, LongValue.of(1), LongValue.of(2), LongValue.of(3))).isTrue();
			assertThat(validate(shape, LongValue.of(1), LongValue.of(2))).isFalse();

		});
	}

}
