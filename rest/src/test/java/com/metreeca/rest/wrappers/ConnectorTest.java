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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.assertj.core.api.Condition;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.tray.Tray.tool;

import static org.assertj.core.api.Assertions.assertThat;


final class ConnectorTest {

	private static final Statement data=statement(RDF.NIL, RDF.VALUE, RDF.FIRST);


	private void exec(final Runnable task) {
		new Tray().exec(task).clear();
	}


	@Nested final class Connection {

		private Wrapper wrapper(final Consumer<RepositoryConnection> task) {
			return handler -> request -> consumer -> tool(Graph.graph()).query(connection -> {

				task.accept(connection);

				handler.handle(request).accept(consumer);

			});
		}

		private Handler handler(final Consumer<RepositoryConnection> task) {
			return request -> consumer -> tool(Graph.graph()).query(connection -> {

				task.accept(connection);

				request.reply(response -> response.status(Response.OK)).accept(consumer);

			});
		}


		private Condition<Response> processed() {
			return new Condition<>(response -> response.status() != 0, "handler executed");
		}


		@Test void testShareConnection() {

			final AtomicReference<RepositoryConnection> wrapperConnection=new AtomicReference<>();
			final AtomicReference<RepositoryConnection> handlerConnection=new AtomicReference<>();

			exec(() -> new Connector()

					.wrap(wrapper(wrapperConnection::set))
					.wrap(handler(handlerConnection::set))

					.handle(new Request())

					.accept(response -> {

						assertThat(response).satisfies(processed());

						assertThat(wrapperConnection.get())
								.as("connection shared").isEqualTo(handlerConnection.get());

					})
			);
		}

		@Test void testAvoidTransactionOnSafeRequests() {
			exec(() -> new Connector()

					.wrap(handler(connection -> assertThat(connection.isActive()).isFalse()))

					.handle(new Request().method(Request.GET))

					.accept(response -> assertThat(response).satisfies(processed()))

			);
		}

		@Test void testOpenTransactionOnUnsafeRequests() {
			exec(() -> new Connector()

					.wrap(handler(connection -> assertThat(connection.isActive()).isTrue()))

					.handle(new Request().method(Request.POST))

					.accept(response -> assertThat(response).satisfies(processed()))

			);
		}

		@Test void testCommitTransactionOnSuccessfulResponses() {
			exec(() -> new Connector()

					.wrap(handler(connection -> connection.add(data)))

					.handle(new Request().method(Request.POST))

					.accept(response -> {

						assertThat(response).satisfies(processed());

						tool(Graph.graph()).query(connection -> {
							assertThat(connection.hasStatement(data, true))
									.as("transaction committed").isTrue();
						});

					})

			);
		}

		@Test void testRollBackTransactionOnUnsuccessfulResponses() {
			exec(() -> new Connector()

					.wrap((Wrapper)handler -> request ->
							handler.handle(request).map(response -> response.status(Response.BadRequest))
					)

					.wrap(handler(connection -> connection.add(data)))

					.handle(new Request().method(Request.POST))

					.accept(response -> {

						assertThat(response).satisfies(processed());

						tool(Graph.graph()).query(connection -> {
							assertThat(connection.hasStatement(data, true))
									.as("transaction rolled back").isFalse();
						});

					})

			);
		}

	}

	@Nested final class SPARQLFilters {

		@Test void testConfigureRequest() {
			exec(() -> assertThat(Connector.<Request>query(

					sparql("\n"
							+"construct { \n"
							+"\n"
							+"\t?this\n"
							+"\t\t:time $time;\n"
							+"\t\t:stem $stem;\n"
							+"\t\t:name $name;\n"
							+"\t\t:task $task;\n"
							+"\t\t:base $base;\n"
							+"\t\t:item $item;\n"
							+"\t\t:user $user;\n"

							+"\t\t:custom $custom.\n"
							+"\n"
							+"} where {}\n"
					),

					(request, query) -> query.setBinding("custom", literal(123))

					)

							.apply(new Request()

											.method(Request.POST)
											.base(Base)
											.path("/test/request"),

									new LinkedHashModel(set(data))
							)
					)

							.as("bindings configured")
							.hasSubset(decode("\n"
									+"<test/request>\n"
									+"\t:stem <test/>;\n"
									+"\t:name 'request';\n"
									+"\t:task 'POST';\n"
									+"\t:base <>;\n"
									+"\t:item <test/request>;\n"
									+"\t:user form:none.\n"
							))

							.as("timestamp configured")
							.hasStatement(item("test/request"), term("time"), null)

							.as("custom settings applied")
							.hasStatement(item("test/request"), term("custom"), literal(123))

							.as("existing statements forwarded")
							.hasSubset(data)

			);
		}

		@Test void testConfigureResponse() {
			exec(() -> assertThat(Connector.<Response>query(

					sparql("\n"
							+"construct { \n"
							+"\n"
							+"\t?this\n"
							+"\t\t:time $time;\n"
							+"\t\t:stem $stem;\n"
							+"\t\t:name $name;\n"
							+"\t\t:task $task;\n"
							+"\t\t:base $base;\n"
							+"\t\t:item $item;\n"
							+"\t\t:user $user;\n"
							+"\t\t:code $code;\n"

							+"\t\t:custom $custom.\n"
							+"\n"
							+"} where {}\n"
					),

					(request, query) -> query.setBinding("custom", literal(123))

					)

							.apply(new Response(new Request()

											.method(Request.POST)
											.base(Base)
											.path("/test/request"))

									.status(Response.OK)
									.header("Location", Base+"test/response"),

									new LinkedHashModel(set(data))
							)
					)

							.as("bindings configured")
							.hasSubset(decode("\n"
									+"<test/response>\n"
									+"\t:stem <test/>;\n"
									+"\t:name 'response';\n"
									+"\t:task 'POST';\n"
									+"\t:base <>;\n"
									+"\t:item <test/request>;\n"
									+"\t:user form:none;\n"
									+"\t:code 200.\n"
							))

							.as("timestamp configured")
							.hasStatement(item("test/response"), term("time"), null)

							.as("custom settings applied")
							.hasStatement(item("test/response"), term("custom"), literal(123))

							.as("existing statements forwarded")
							.hasSubset(data)

			);
		}

	}

}
