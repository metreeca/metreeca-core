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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import static com.metreeca.rest.bodies.RDFBody.rdf;
import static com.metreeca.rest.bodies.TextBody.text;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;


public final class HandlerTest {

	@SafeVarargs public static Handler echo(final Function<Response, Response>... tasks) {
		return request -> request.reply(response -> response

				.status(Response.OK)

				.shape(request.shape())
				.headers(request.headers())

				.map(r -> request.body(rdf()).fold(v -> r.body(rdf(), v), e -> e.equals(Body.Missing) ? r : r.map(e)))

				.map(stream(tasks).reduce(identity(), Function::andThen))

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPreserverChainedWrapperOrder() {
		echo()

				.with(handler -> request -> handler.handle(request.header("+Chain", "1")))
				.with(handler -> request -> handler.handle(request.header("+Chain", "2")))
				.with(handler -> request -> handler.handle(request.header("+Chain", "3")))

				.handle(new Request())

				.accept(response -> assertThat(response.headers("Chain"))
						.containsExactly("1", "2", "3")
				);
	}


	@Test void testResultStreaming() {

		final Collection<String> transaction=new ArrayList<>();

		final Handler handler=request -> consumer -> {

			transaction.add("begin");

			request.reply(response -> response.body(text(), "inside")).accept(consumer);

			transaction.add("commit");

		};

		handler.handle(new Request()).accept(response -> {
			transaction.add(response.body(text()).value().orElse(""));
		});

		assertThat(transaction).containsExactly("begin", "inside", "commit");
	}

}
