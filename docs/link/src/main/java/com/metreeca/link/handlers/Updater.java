/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.link.handlers;

import com.metreeca.link.Handler;
import com.metreeca.link.Request;
import com.metreeca.link.Response;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.metreeca.spec.things.Values.iri;


/**
 * SPARQL post-processor.
 */
public final class Updater implements Handler {

	private final Graph graph;

	private final Handler handler;
	private final Map<String, String> updates;


	public Updater(final Tool.Loader tools, final Handler handler, final Map<String, String> updates) {

		if ( tools == null ) {
			throw new NullPointerException("null tools");
		}

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		if ( updates == null ) {
			throw new NullPointerException("null updates");
		}

		if ( updates.containsKey(null) || updates.containsKey("")) {
			throw new NullPointerException("null or empty update method");
		}

		if ( updates.containsValue(null) || updates.containsValue("")) {
			throw new IllegalArgumentException("nulll or empty update script");
		}

		this.graph=tools.get(Graph.Tool);

		this.handler=handler;
		this.updates=new HashMap<>(updates);
	}


	@Override public void handle(final Tool.Loader tools,
			final Request request, final Response response, final BiConsumer<Request, Response> sink) {

		final Handler chain=handler
				.chain(handler(updates.get(request.getMethod())))
				.chain(request.isSafe() ? Handler.Empty : handler(updates.get(Request.ANY)));

		if ( chain.equals(handler) ) { handler.handle(tools, request, response, sink); } else {

			graph.update(connection -> { // inside a single transaction

				chain.handle(tools, request, response, sink);

				return null;

			});

		}
	}


	private Handler handler(final String sparql) {
		return sparql == null ? Handler.Empty : (tools, request, response, sink) -> {
			if ( response.getStatus()/100 != 2 ) { sink.accept(request, response); } else {
				try {

					request.map(graph).update(connection -> {

						final IRI target=iri(response.getHeader("Location").orElseGet(request::getTarget));

						final Update update=connection.prepareUpdate(QueryLanguage.SPARQL, sparql, request.getBase());

						update.setBinding("this", target);
						update.execute();

						// !!! handle IRI renaming from script

						sink.accept(request, response);

						return null;

					});

				} catch ( final MalformedQueryException e ) { // !!! abort/rollback wrapping graph transaction

					sink.accept(request, new Response().setStatus(Response.InternalServerError)
							.setHeader("Content-Type", "text-plain")
							.setText("syntax error in update script")
							.setCause(e));

				} catch ( final UpdateExecutionException e ) { // !!! abort/rollback wrapping graph transaction

					sink.accept(request, new Response().setStatus(Response.InternalServerError)
							.setHeader("Content-Type", "text-plain")
							.setText("failed update script")
							.setCause(e));

				}
			}

		};
	}

}
