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

package com.metreeca.rest;

import com.metreeca.tree.Shape;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import static com.metreeca.rest.formats.DataFormat.data;
import static com.metreeca.rest.formats.TextFormat.text;
import static com.metreeca.rest.services.Logger.clip;
import static com.metreeca.tree.shapes.And.and;

import static org.assertj.core.api.Assertions.fail;


public abstract class MessageAssert<A extends MessageAssert<A, T>, T extends Message<T>> extends AbstractAssert<A, T> {

	@SuppressWarnings("unchecked") public static <T extends Message<T>> MessageAssert<?, ?> assertThat(final Message<?> message) {

		final class WorkAssert extends MessageAssert<WorkAssert, T> {

			private WorkAssert(final T actual) { super(actual, WorkAssert.class); }

		}

		return new WorkAssert((T)message);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected MessageAssert(final T actual, final Class<A> type) {
		super(actual, type);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public A hasItem(final String item) {

		isNotNull();

		if ( !Objects.equals(actual.item(), item) ) {
			failWithMessage("expected message to have <%s> item but has <%s>", item, actual.item());
		}

		return myself;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public A hasHeader(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		isNotNull();

		final Collection<String> values=actual.headers(name);

		if ( values.isEmpty() ) {
			failWithMessage("expected message to have <%s> headers but has none", name);
		}

		return myself;
	}

	public A hasHeader(final String name, final String value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		isNotNull();

		final String found=actual.header(name).orElse(null);

		if ( !value.equals(found) ) {
			failWithMessage(
					"expected response to have <%s> header with value <%s> but found <%s>",
					name, value, found
			);
		}

		return myself;
	}

	public A hasHeader(final String name, final Consumer<String> assertions) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( assertions == null ) {
			throw new NullPointerException("null assertions");
		}

		isNotNull();

		final String value=actual.header(name).orElse(null);


		if ( value == null ) {
			failWithMessage("expected message to have <%s> headers but has none", name);
		}

		assertions.accept(value);

		return myself;
	}

	public A doesNotHaveHeader(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		isNotNull();

		final Collection<String> values=actual.headers(name);

		if ( !values.isEmpty() ) {
			failWithMessage("expected response to have no <%s> headers but has <%s>", name, values);
		}

		return myself;
	}


	public A hasHeaders(final String name, final String... values) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		isNotNull();

		Assertions.assertThat(actual.headers(name))
				.as("<%s> message headers", name)
				.containsExactly(values);

		return myself;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public A hasShape() {

		isNotNull();

		final Shape shape=actual.shape();

		if ( and().equals(shape) ) {
			failWithMessage("expected message to have a shape but has none", shape);
		}

		return myself;
	}

	public A hasShape(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		isNotNull();

		if ( !actual.shape().equals(shape) ) {
			failWithMessage("shape message to have shape <%s> but has <%s>", shape, actual.shape());
		}

		return myself;
	}

	public A doesNotHaveShape() {

		isNotNull();

		final Shape shape=actual.shape();

		if ( !and().equals(shape)) {
			failWithMessage("expected message to have no shape but has <%s>", shape);
		}

		return myself;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public A hasBody(final Format<?> format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		return hasBody(format, body -> {});
	}

	public <V> A hasBody(final Format<V> body, final Consumer<V> assertions) {

		if ( body == null ) {
			throw new NullPointerException("null body");
		}

		if ( assertions == null ) {
			throw new NullPointerException("null assertions");
		}

		isNotNull();

		return actual.body(body).fold(

				value -> {

					assertions.accept(value);

					return myself;

				},

				error -> fail(
						"expected message to have a <%s> body but was unable to retrieve one (%s)",
						body.getClass().getSimpleName(), error
				)
		);
	}


	public A doesNotHaveBody() {

		isNotNull();

		actual.body(data()).value().ifPresent(data -> {
			if ( data.length > 0 ) {
				failWithMessage("expected empty body but had binary body of length <%d>", data.length);
			}
		});

		actual.body(text()).value().ifPresent(text -> {
			if ( !text.isEmpty() ) {
				failWithMessage(
						"expected empty body but had textual body of length <%d> (%s)",
						text.length(), clip(text)
				);
			}
		});

		return myself;
	}

	public A doesNotHaveBody(final Format<?> body) {

		if ( body == null ) {
			throw new NullPointerException("null body");
		}

		isNotNull();

		return actual.body(body).fold(
				value -> fail("expected message to have no <%s> body but has one"),
				error -> myself
		);
	}

}
