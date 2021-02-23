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


import com.metreeca.rest.formats.InputFormat;
import com.metreeca.rest.formats.TextFormat;

import org.assertj.core.api.Assertions;

import java.io.*;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.function.Function.identity;


public final class RequestAssert extends MessageAssert<RequestAssert, Request> {

	public static RequestAssert assertThat(final Request request) {

		if ( request != null ) {

			request.body(InputFormat.input(), (request.body(InputFormat.input()).fold(e -> Xtream::input, source -> { // cache input

				try ( final InputStream stream=source.get() ) {

					final byte[] data=Xtream.data(stream);

					return () -> new ByteArrayInputStream(data);

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}

			})));

			final StringBuilder builder=new StringBuilder(2500);

			builder.append(request.method()).append(' ').append(request.item()).append('\n');

			request.headers().forEach((name, values) -> values.forEach(value ->
					builder.append(name).append(": ").append(value).append('\n')
			));

			builder.append('\n');


			final String text=request.body(TextFormat.text()).fold(e -> "", identity());

			if ( !text.isEmpty() ) {

				final int limit=builder.capacity();

				builder
						.append(text.length() <= limit ? text : text.substring(0, limit)+"\n⋮")
						.append("\n\n");
			}

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
