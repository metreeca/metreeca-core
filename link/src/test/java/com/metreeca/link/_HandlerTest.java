/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.link;


import com.metreeca.tray.IO;
import com.metreeca.tray.Tool;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.metreeca.spec.things.ValuesTest.repository;


public final class _HandlerTest {

	private _HandlerTest() {} // a utility class


	public static void tools(final Consumer<Tool.Loader> task) {
		repository(repository -> {

			final Tray manager=Tray.tray().set(Graph.Tool, tools ->
					new Graph("Test Repository", IsolationLevels.SERIALIZABLE, () -> repository) {});

			try {

				task.accept(manager);

				return null;

			} finally {
				manager.clear();
			}

		});
	}


	public static void response(final Tool.Loader tools, final _Handler handler, final _Request request, final BiConsumer<_Request, _Response> sink) {

		final _Response response=new _Response();

		final int delivered=599; // the last valid response code

		handler.chain(_HandlerTest::sniff).handle(tools, request, response, (_request, _response) -> {
			try { sink.accept(_request, _response); } finally { _response.setStatus(delivered); }
		});

		if ( response.getStatus() != delivered ) {
			throw new AssertionError("response not delivered");
		}
	}

	public static void exception(final Tool.Loader tools, final _Handler handler, final _Request request, final BiConsumer<_Request, _LinkException> sink) {
		try {

			handler.handle(tools, request, new _Response(), (_request, _response) -> sink.accept(_request, new _LinkException(0)));

		} catch ( final _LinkException e ) {

			sink.accept(request, e);

		}
	}


	public static Collection<Statement> model(final Graph graph, final Resource... contexts) {
		return graph.browse(connection -> {

			final StatementCollector collector=new StatementCollector();

			connection.export(collector, contexts);

			return collector.getStatements();

		});
	}

	public static Graph model(final Graph graph, final Iterable<Statement> model, final Resource... contexts) {
		return graph.update(connection -> {

			connection.add(model, contexts);

			return graph;

		});
	}


	private static void sniff(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		final StringBuilder headers=new StringBuilder(100);

		for (final Map.Entry<String, Collection<String>> entry : response.getHeaders().entrySet()) {
			for (final String value : entry.getValue()) {
				headers.append(String.format("%s: %s\n", entry.getKey(), value));
			}
		}

		final byte[] body=response.getData();

		tools.get(Trace.Tool).info(null, String.format("HTTP Response Code %d\n%s\n%s\n---------------------------",
				response.getStatus(), headers, new String(body, IO.UTF8)));

		response.setBody(out -> {
			try {
				out.write(body);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});

		sink.accept(request, response);

	}

}
