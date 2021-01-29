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

import com.metreeca.rest.formats.*;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.function.Function.identity;


public final class ResponseAssert extends MessageAssert<ResponseAssert, Response> {

	public static ResponseAssert assertThat(final Response response) {

		if ( response != null ) {

			response.body(OutputFormat.output()).accept(e -> {}, target -> {

				final byte[] data;

				try ( final ByteArrayOutputStream out=new ByteArrayOutputStream(1000) ) {

					target.accept(out);

					data=out.toByteArray();

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}

				response.body(OutputFormat.output(), output -> DataFormat.data(output, data)); // cache output
				response.body(InputFormat.input(), () -> new ByteArrayInputStream(data)); // make output readable for testing

			});

			final StringBuilder builder=new StringBuilder(2500);

			builder.append(response.status()).append('\n');

			response.headers().forEach((name, values) -> values.forEach(value ->
					builder.append(name).append(": ").append(value).append('\n')
			));

			builder.append('\n');

			final String text=response.body(TextFormat.text()).fold(e -> "", identity());

			if ( !text.isEmpty() ) {

				final int limit=builder.capacity();

				builder
						.append(text.length() <= limit ? text : text.substring(0, limit)+"\n⋮")
						.append("\n\n");
			}

			Logger.getLogger(response.getClass().getName()).log(
					response.status() < 400 ? Level.INFO : response.status() < 500 ? Level.WARNING : Level.SEVERE,
					builder.toString(),
					response.cause().orElse(null)
			);

		}

		return new ResponseAssert(response);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ResponseAssert(final Response actual) {
		super(actual, ResponseAssert.class);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public ResponseAssert isSuccess() {

		isNotNull();

		if ( !actual.success() ) {
			failWithMessage("expected response to be success but was <%d>", actual.status());
		}

		return this;
	}

	public ResponseAssert hasStatus(final int expected) {

		isNotNull();

		if ( actual.status() != expected ) {
			failWithMessage("expected response status to be <%d> was <%d>", expected, actual.status());
		}

		return this;
	}

	public ResponseAssert hasCause(final Class<? extends Throwable> expected) {

		isNotNull();

		if ( expected.isInstance(actual.cause()) ) {
			failWithMessage("expected response to have cause of class <%s>", expected);
		}

		return this;
	}

}
