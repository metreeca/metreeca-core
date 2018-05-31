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


import com.metreeca.link._work.Rewriter;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;
import com.metreeca.tray.sys.Trace;
import com.metreeca.tray.sys.Trace.Level;

import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.regex.Pattern;

import static com.metreeca.link.Handler.error;
import static com.metreeca.link.Wrapper.wrapper;
import static com.metreeca.link.handlers.Router.router;
import static com.metreeca.tray.Tray.tool;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * Linked data server {thread-safe}.
 *
 * <ul>
 *
 * <li>default pre/post-processing</li>
 *
 * <li>IRI rewriting</li>
 *
 * <li>error handling</li>
 *
 * </ul>
 */
public final class Server implements Handler {

	private static final Pattern CharsetPattern=Pattern.compile(";\\s*charset\\s*=.*$");


	public static final Tool<Server> Tool=tools -> server();


	public static Server server() {
		return new Server(router());
	}

	public static Server server(final Wrapper... wrappers) {

		if ( wrappers == null ) {
			throw new NullPointerException("null wrappers");
		}

		return new Server(Arrays.stream(wrappers).reduce(wrapper(), (chain, wrapper) -> {

			if ( wrapper == null ) {
				throw new NullPointerException("null wrapper");
			}

			return chain.wrap(wrapper);

		}).wrap(router()));
	}

	public static Server server(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return new Server(handler);
	}


	private final Handler handler;

	private final String base=tool(Setup.Tool).get(Setup.BaseProperty).orElse("");

	private final Graph graph=tool(Graph.Tool);
	private final Trace trace=tool(Trace.Tool);


	private Server(final Handler handler) {
		this.handler=handler;
	}


	@Override public void handle(final Request request, final Response response) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		if ( response == null ) {
			throw new NullPointerException("null response");
		}


		try (final RepositoryConnection ignored=graph.connect()) { // process the request on a single connection

			handler // wrap on the fly to make the original handler available to overridden Handler.wrap()

					.wrap(rewriter()).wrap(postprocessor()) // outside rewriter: sees original URLs

					.handle(request, response);

		} catch ( final RuntimeException e ) {

			trace.error(this, format("%s %s > internal error", request.method(), request.focus()), e);

			if ( !response.committed() ) {
				response.status(Response.InternalServerError).cause(e).json(error(
						"exception-untrapped",
						"unable to process request: see server logs for details"
				));
			}

		}
	}

	@Override public Server wrap(final Wrapper wrapper) {

		if ( wrapper == null ) {
			throw new NullPointerException("null wrapper");
		}

		return new Server(wrapper.wrap(handler));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper postprocessor() {
		return handler -> (request, response) -> handler.handle(

				writer ->

						writer.copy(request).done(),

				reader -> {

					final int status=reader.status();
					final Throwable cause=reader.cause();

					trace.entry(status < 400 ? Level.Info : status < 500 ? Level.Warning : Level.Error,
							this, format("%s %s > %d", request.method(), request.focus(), status), cause);

					response.copy(reader);

					// if no charset is specified, add a default one to prevent the container from adding its own…

					reader.header("Content-Type")
							.filter(type -> !CharsetPattern.matcher(type).find())
							.filter(type -> type.startsWith("text/") || type.equals("application/json"))
							.ifPresent(type -> response.header("Content-Type", type+";charset=UTF-8"));

					if ( status == Response.OK ) {
						response.header("Vary", "Accept", "Prefer", "");
					}

					if ( request.interactive() ) { // LDP server base
						// !!! response.header("Set-Cookie", BaseCookie+"="+request.base()+";path=/");
					}

					response.done();

				}

		);
	}

	private Wrapper rewriter() {
		return handler -> (request, response) -> handler.handle(

				writer -> {

					// !!! limit input size? e.g. to prevent DoS attacks in cloud

					final Rewriter rewriter=Rewriter.rewriter(request.base(), base);

					writer.copy(request)

							.user(rewriter.internal(request.user()))
							.roles(request.roles().stream().map(rewriter::internal).collect(toSet()))

							.base(rewriter.internal(request.base()))

							.query(rewriter.internal(request.query()))

							.parameters(request.parameters().map(h -> new SimpleImmutableEntry<>(h.getKey(),
									h.getValue().stream().map(rewriter::internal).collect(toList()))))

							.headers(request.headers().map(h -> new SimpleImmutableEntry<>(h.getKey(),
									h.getValue().stream().map(rewriter::internal).collect(toList()))))

							.body(() -> rewriter.internal(request.input()), () -> rewriter.internal(request.reader()));
				},

				reader -> {

					final Rewriter rewriter=Rewriter.rewriter(request.base(), base);

					response.copy(reader)

							.headers(reader.headers().map(h -> new SimpleImmutableEntry<>(h.getKey(),
									h.getValue().stream().map(rewriter::external).collect(toList()))));

					if ( reader.binary() ) {

						response.output(output -> reader.output(rewriter.external(output)));

					} else if ( reader.textual() ) {

						response.writer(writer -> reader.writer(rewriter.external(writer)));

					} else {

						response.done();

					}

				}

		);
	}

}
