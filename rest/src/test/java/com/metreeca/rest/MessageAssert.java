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

package com.metreeca.rest;

import com.metreeca.form.Shape;

import org.assertj.core.api.AbstractAssert;

import java.util.Collection;
import java.util.function.Consumer;

import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.rest.bodies.DataBody.data;
import static com.metreeca.rest.bodies.TextBody.text;
import static com.metreeca.tray.sys.Trace.clip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


public abstract class MessageAssert<A extends MessageAssert<A, T>, T extends Message<T>> extends AbstractAssert<A, T> {

	protected MessageAssert(final T actual, final Class<A> type) {
		super(actual, type);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public A hasHeader(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		isNotNull();

		final Collection<String> values=actual.headers(name);

		if ( values.isEmpty() ) {
			failWithMessage("expected response to have <%s> headers but has none", name);
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

		assertThat(actual.headers(name))
				.as("<%sh> message headers", name)
				.containsExactly(values);

		return myself;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public A hasShape() {

		isNotNull();

		final Shape shape=actual.shape();

		if ( pass(shape) ) {
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

		if ( !pass(shape) ) {
			failWithMessage("expected message to have no shape but has <%s>", shape);
		}

		return myself;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	public A hasBody(final Body<?> format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		return hasBody(format, body -> {});
	}

	public <V> A hasBody(final Body<V> body, final Consumer<V> assertions) {

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
						"expected response to have a <%s> body but was unable to retrieve one (%s)",
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

	public A doesNotHaveBody(final Body<?> body) {

		if ( body == null ) {
			throw new NullPointerException("null body");
		}

		isNotNull();

		return actual.body(body).fold(
				value -> fail("expected response to have no <%s> body but has one"),
				error -> myself
		);
	}

}
