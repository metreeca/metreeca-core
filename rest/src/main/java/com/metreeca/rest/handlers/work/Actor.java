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

package com.metreeca.rest.handlers.work;

import com.metreeca.rest.*;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Collection;

import static com.metreeca.form.Shape.pass;
import static com.metreeca.form.things.Strings.indent;
import static com.metreeca.tray.Tray.tool;


/**
 * Resource actor.
 *
 * <p>Handles actions on linked data resources.</p>
 *
 * <p>The abstract base class:</p>
 *
 * <ul>
 *
 * <li>!!!</li>
 *
 * </ul>
 */
public abstract class Actor implements Wrapper, Handler {

	protected static Wrapper query(final boolean accepted) {
		return handler -> request -> accepted || request.query().isEmpty() ? handler.handle(request)
				: request.reply(new Failure().status(Response.BadRequest).cause("unexpected query parameters"));
	}


	protected static Wrapper target(final Wrapper container, final Wrapper resource) {
		return new Wrapper() {

			@Override public Wrapper wrap(final Wrapper wrapper) {
				return target(container.wrap(wrapper), resource.wrap(wrapper));
			}

			@Override public Handler wrap(final Handler handler) {
				return target(container.wrap(handler), resource.wrap(handler));
			}

		};
	}

	protected static Handler target(final Handler container, final Handler resource) {
		return request -> (request.container() ? container : resource).handle(request);
	}

	protected static Handler driver(final Handler simple, final Handler shaped) {
		return request -> (pass(request.shape()) ? simple : shaped).handle(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	private final Wrapper wrapper;
	private final Handler handler;

	private final Handler delegate;


	protected Actor(final Wrapper wrapper, final Handler handler) {

		if ( wrapper == null ) {
			throw new NullPointerException("null wrapper");
		}

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		this.wrapper=wrapper;
		this.handler=handler;

		this.delegate=wrapper.wrap(handler);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return wrapper.wrap(handler);
	}

	@Override public Wrapper wrap(final Wrapper wrapper) {
		return new Actor(wrapper.wrap(wrapper), handler) {};
	}


	@Override public Responder handle(final Request request) {
		return delegate.handle(request);
	}


	/// !!! migrate ////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Trace trace=tool(Trace.Factory);

	protected Collection<Statement> trace(final Collection<Statement> model) {

		trace.entry(Trace.Level.Debug, this, () -> {

			try (final StringWriter writer=new StringWriter()) {

				Rio.write(model, new TurtleWriter(writer));

				return "processing model\n"+indent(writer, true);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		}, null);

		return model;
	}

}
