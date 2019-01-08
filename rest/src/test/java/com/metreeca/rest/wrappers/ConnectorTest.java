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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.assertj.core.api.Condition;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.tray.Tray.tool;

import static org.assertj.core.api.Assertions.assertThat;


final class ConnectorTest {

	private static final Statement data=statement(RDF.NIL, RDF.VALUE, RDF.FIRST);

	private void tray(final Runnable task) {
		new Tray().exec(task).clear();
	}


	private Wrapper wrapper(final Consumer<RepositoryConnection> task) {
		return handler -> request -> consumer -> tool(Graph.Factory).query(connection -> {

			task.accept(connection);

			handler.handle(request).accept(consumer);

		});
	}

	private Handler handler(final Consumer<RepositoryConnection> task) {
		return request -> consumer -> tool(Graph.Factory).query(connection -> {

			task.accept(connection);

			request.reply(response -> response.status(Response.OK)).accept(consumer);

		});
	}


	private Condition<Response> processed() {
		return new Condition<>(response -> response.status() != 0, "handler executed");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testShareConnection() {

		final AtomicReference<RepositoryConnection> wrapperConnection=new AtomicReference<>();
		final AtomicReference<RepositoryConnection> handlerConnection=new AtomicReference<>();

		tray(() -> new Connector()

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
		tray(() -> new Connector()

				.wrap(handler(connection -> assertThat(connection.isActive()).isFalse()))

				.handle(new Request().method(Request.GET))

				.accept(response -> assertThat(response).satisfies(processed()))

		);
	}

	@Test void testOpenTransactionOnUnsafeRequests() {
		tray(() -> new Connector()

				.wrap(handler(connection -> assertThat(connection.isActive()).isTrue()))

				.handle(new Request().method(Request.POST))

				.accept(response -> assertThat(response).satisfies(processed()))

		);
	}

	@Test void testCommitTransactionOnSuccessfulResponses() {
		tray(() -> new Connector()

				.wrap(handler(connection -> connection.add(data)))

				.handle(new Request().method(Request.POST))

				.accept(response -> {

					assertThat(response).satisfies(processed());

					tool(Graph.Factory).query(connection -> {
						assertThat(connection.hasStatement(data, true))
								.as("transaction committed").isTrue();
					});

				})

		);
	}

	@Test void testRollBackTransactionOnUnsuccessfulResponses() {
		tray(() -> new Connector()

				.wrap((Wrapper)handler -> request ->
						handler.handle(request).map(response -> response.status(Response.BadRequest))
				)

				.wrap(handler(connection -> connection.add(data)))

				.handle(new Request().method(Request.POST))

				.accept(response -> {

					assertThat(response).satisfies(processed());

					tool(Graph.Factory).query(connection -> {
						assertThat(connection.hasStatement(data, true))
								.as("transaction rolled back").isFalse();
					});

				})

		);
	}

}
