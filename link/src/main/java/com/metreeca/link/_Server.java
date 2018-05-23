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

import com.metreeca.link.handlers.Dispatcher;
import com.metreeca.link.handlers.Router;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.StringWriter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import static com.metreeca.spec.things.Maps.entry;
import static com.metreeca.spec.things.Maps.map;
import static com.metreeca.spec.things.Values.format;
import static com.metreeca.spec.things.Values.iri;

import static java.util.Arrays.asList;


public final class _Server {

	private static final Logger logger=Logger.getLogger(_Server.class.getName()); // !!! migrate to Trace


	/**
	 * Cookie name used to advertise the absolute server base IRI to interactive apps.
	 */
	private static final String BaseCookie=Link.class.getPackage().getName();

	/**
	 * Cookie name used to advertise the current user IRI to interactive apps.
	 */
	private static final String UserCookie=BaseCookie+".user";  // !!! replace with complete meta info on root?


	public static final Tool<_Server> Tool=_Server::new;


	private final String canonical;

	private final Graph graph;

	private final _Handler handler;


	private String base; // the base IRI for the active request; {@code null} if no request is active

	private final Queue<BiConsumer<String, String>> hooks=new ArrayDeque<>(); // pending on demand initialization hooks


	public _Server(final Tool.Loader tools) {
		this(tools, new Router());
	}

	public _Server(final Tool.Loader tools, final _Handler... handlers) {
		this(tools, asList(handlers));
	}

	public _Server(final Tool.Loader tools, final Iterable<_Handler> handlers) {

		if ( tools == null ) {
			throw new NullPointerException("null tools");
		}

		if ( handlers == null ) {
			throw new NullPointerException("null com.metreeca.next.handlers");
		}

		final Setup setup=tools.get(Setup.Tool);

		this.canonical=setup.get(Setup.BaseProperty).orElse(null);

		this.graph=tools.get(Graph.Tool);

		final _Gate gate=tools.get(_Gate.Tool);

		this.handler=StreamSupport

				.stream(handlers.spliterator(), false).reduce(gate::authorize, _Handler::chain)

				.chain(new Dispatcher(map( // default method post-processors

						entry(_Request.DELETE, this::delete), entry(_Request.ANY, _Handler.Empty)

				)))

				.chain(this::defaults) // default interaction post-processor
				.chain(gate::authenticate);
	}


	/**
	 * Registers an on demand initialization hook.
	 *
	 * <p>Initialization hooks are invoked as soon a request is active, with the following arguments:</p>
	 *
	 * <ol>
	 *
	 * <li>the absolute base URL of this server as {@linkplain _Request#getBase() derived} from the request
	 * object;</li>
	 *
	 * <li>the absolute canonical base URL of this server, if defined in the {@linkplain Setup#BaseProperty setup},
	 * or the base URL derived from the request object, otherwise.</li>
	 *
	 * </ol>
	 *
	 * @param hook the on demand initialization hook to be registered
	 *
	 * @return this server
	 */
	public _Server hook(final BiConsumer<String, String> hook) {

		if ( hook == null ) {
			throw new NullPointerException("null hook");
		}

		if ( !hooks.contains(hook) ) {

			hooks.add(hook); // queue hook

			if ( base != null && hooks.size() == 1 ) { // first pending hook inside active request > start processing
				hooks();
			}

		}

		return this;
	}

	private void hooks() {
		while ( !hooks.isEmpty() ) {
			hooks.remove().accept(base, canonical == null || canonical.isEmpty() ? base : canonical);
		}
	}


	public _Server serve(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) { // !!! refactor

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		if ( response == null ) {
			throw new NullPointerException("null response");
		}

		if ( sink == null ) {
			throw new NullPointerException("null sink");
		}

		try {

			base=request.getBase(); // mark active request

			hooks(); // process pending hooks

			handler.handle(tools, request, response, sink);

		} catch ( final _LinkException e ) { // !!! remove

			defaults(tools, request, response.setStatus(e.getStatus()).setText(e.getReport()).setCause(e.getCause()), sink);

		} catch ( final RuntimeException e ) {

			defaults(tools, request, response.setStatus(_Response.InternalServerError).setText("unable to process request: see server logs for details").setCause(e), sink);

		} finally {

			base=null;

		}

		return this;
	}


	//// Default Post-Processors ///////////////////////////////////////////////////////////////////////////////////////

	private void delete(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		if ( response.getStatus()/100 != 2 ) { sink.accept(request, response); } else {

			final IRI target=iri(request.getTarget());

			// look for leftover references // !!! ignored archived and housekeeping references (how to test?)

			final Collection<Statement> cell=new ArrayList<>();

			request.map(graph).browse(connection -> {

				try (final RepositoryResult<Statement> direct=connection.getStatements(target, null, null, true)) {
					while ( direct.hasNext() ) { cell.add(direct.next()); }
				}

				try (final RepositoryResult<Statement> inverse=connection.getStatements(null, null, target, true)) {
					while ( inverse.hasNext() ) { cell.add(inverse.next()); }
				}

				return this;

			});

			if ( cell.isEmpty() ) { sink.accept(request, response); } else { // report leftovers

				final StringWriter references=new StringWriter(100);

				Rio.write(cell, references, RDFFormat.TURTLE);

				sink.accept(request, new _Response().setStatus(_Response.InternalServerError).setText("resource still referenced after deletion").setCause(new IllegalStateException("leftover references after "+format(target)+" deletion:\n"+references)));

			}

		}
	}

	private void defaults(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		final int status=response.getStatus();

		if ( status == _Response.OK ) {
			response.addHeader("Vary", "Accept, Prefer");
		}

		if ( request.isInteractive() ) { // LDP server base
			response.addHeader("Set-Cookie", BaseCookie+"="+request.getBase()+";path=/");
			response.addHeader("Set-Cookie", UserCookie+"="+request.getUser()+";path=/");
		}

		logger.log(status < 400 ? Level.INFO : status < 500 ? Level.WARNING : Level.SEVERE, String.format("%s %s > %d", request.getMethod(), request.getTarget(), status), response.getCause());

		sink.accept(request, response);
	}

}
