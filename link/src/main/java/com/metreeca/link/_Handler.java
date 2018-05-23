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

import com.metreeca.tray.Tool;

import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.function.BiConsumer;


@FunctionalInterface public interface _Handler {

	public static final _Handler Empty=new _Handler() {

		@Override public void handle(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {
			sink.accept(request, response);
		}

		@Override public _Handler chain(final _Handler handler) {
			return handler;
		}

	};


	public static _Handler sysadm(final _Handler handler) { // !!! remove after testing shape-based authorization

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return (tools, request, response, sink) -> {
			if ( request.isSysAdm() ) {
				handler.handle(tools, request, response, sink);
			} else {
				unauthorized(tools, request, response, sink);
			}
		};
	}


	public static void unsupported(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		// !!! MethodNotAllowed responses MUST include an Allow header
		// !!! (https://tools.ietf.org/html/rfc7231#section-6.5.5)
		// !!! introspection?

		sink.accept(request, response.setStatus(_Response.MethodNotAllowed)
				.setText("unsupported "+request.getMethod()+" request ["+request.getTarget()+"]"));
	}

	public static void unauthorized(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		if ( false ) {

			// An origin server that wishes to "hide" the current existence of a forbidden target resource MAY
			// instead respond with a status code of 404 (Not Found).

			// !!! if enabling this feature make sure user is validated before method (what if the method is not
			// !!! supported: both baseline and user shapes are empty: NotFound or MethodNotAllowed?)

			sink.accept(request, response.setStatus(_Response.NotFound));

		} else if ( request.getUser().equals(RDF.NIL) ) {

			// no WWW-Authenticate header: authentication is managed by the server

			sink.accept(request, response.setStatus(_Response.Unauthorized));

		} else {

			sink.accept(request, response.setStatus(_Response.Forbidden));

		}

	}

	public static void unimplemented(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		sink.accept(request, response.setStatus(_Response.NotImplemented));

	}


	public void handle(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink);


	public default _Handler chain(final _Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return handler.equals(Empty) ? this : (tool, request, response, sink) ->
				handle(tool, request, response, (_request, _response) ->
						handler.handle(tool, _request, _response, sink));
	}

}
