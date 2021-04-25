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

package com.metreeca.rest;

import com.metreeca.rest.formats.DataFormat;
import com.metreeca.rest.formats.TextFormat;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.metreeca.json.Values.clip;

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

	public <V> A hasAttribute(final Supplier<V> factory, final Consumer<V> assertions) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		if ( assertions == null ) {
			throw new NullPointerException("null assertions");
		}

		isNotNull();

		assertions.accept(actual.get(factory));

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

				error -> fail(
						"expected message to have a <%s> body but was unable to retrieve one (%s)",
						body.getClass().getSimpleName(), error
				),

				value -> {

					assertions.accept(value);

					return myself;

				}

		);
	}


	public A doesNotHaveBody() {

		isNotNull();

		actual.body(DataFormat.data()).accept(e -> {}, data -> {

			if ( data.length > 0 ) {
				failWithMessage("expected empty body but had binary body of length <%d>", data.length);
			}

		});

		actual.body(TextFormat.text()).accept(e -> {}, text -> {

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
				error -> myself, value -> fail("expected message to have no <%s> body but has one")
		);
	}

}
