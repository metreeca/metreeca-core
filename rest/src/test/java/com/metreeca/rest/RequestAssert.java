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


import com.metreeca.form.things.Codecs;

import org.assertj.core.api.Assertions;

import java.io.*;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.metreeca.rest.bodies.InputBody.input;
import static com.metreeca.rest.bodies.TextBody.text;


public final class RequestAssert extends MessageAssert<RequestAssert, Request> {

	public static RequestAssert assertThat(final Request request) {

		if ( request != null ) {

			request.pipe(input(), input -> Result.Value(new Supplier<InputStream>() { // cache input

				private byte[] data;

				@Override public InputStream get() {

					if ( data == null ) {
						try (final InputStream in=input.get()) {
							data=Codecs.data(in);
						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}
					}

					return new ByteArrayInputStream(data);
				}

			}));

			final StringBuilder builder=new StringBuilder(2500);

			builder.append(request.method()).append(' ').append(request.item()).append('\n');

			request.headers().forEach((name, values) -> values.forEach(value ->
					builder.append(name).append(": ").append(value).append('\n')
			));

			builder.append('\n');

			request.body(text()).value().ifPresent(text -> {
				if ( !text.isEmpty() ) {

					final int limit=builder.capacity();

					builder
							.append(text.length() <= limit ? text : text.substring(0, limit)+"\n⋮")
							.append("\n\n");
				}
			});

			Logger.getLogger(request.getClass().getName()).log(
					Level.INFO,
					builder.toString()
			);

		}

		return new RequestAssert(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private RequestAssert(final Request actual) {
		super(actual, RequestAssert.class);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public RequestAssert hasParameter(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		isNotNull();

		final Collection<String> values=actual.headers(name);

		if ( values.isEmpty() ) {
			failWithMessage("expected message to have <%s> parameters but has none", name);
		}

		return myself;
	}

	public RequestAssert hasParameter(final String name, final String expected) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( expected == null ) {
			throw new NullPointerException("null expected value");
		}

		isNotNull();

		final String found=actual.parameter(name).orElse(null);

		if ( !expected.equals(found) ) {
			failWithMessage(
					"expected response to have <%s> parameter with value <%s> but found <%s>",
					name, expected, found
			);
		}

		return myself;
	}

	public RequestAssert doesNotHaveParameter(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		isNotNull();

		final Collection<String> values=actual.headers(name);

		if ( !values.isEmpty() ) {
			failWithMessage("expected response to have no <%s> parameters but has <%s>", name, values);
		}

		return myself;
	}


	public RequestAssert hasParameters(final String name, final String... values) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		isNotNull();

		Assertions.assertThat(actual.headers(name))
				.as("<%sh> message parameters", name)
				.containsExactly(values);

		return myself;
	}

}
