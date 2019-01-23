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

package com.metreeca.rest.handlers;

import com.metreeca.rest.*;
import com.metreeca.rest.wrappers.Modulator;
import com.metreeca.rest.wrappers.Processor;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Collection;

import static com.metreeca.form.things.Strings.indent;
import static com.metreeca.tray.Tray.tool;

import static java.util.Arrays.asList;


/**
 * Resource actor.
 *
 * <p>Handles actions on linked data resources.</p>
 *
 * <p>The abstract base class:</p>
 *
 * <ul>
 *
 * <li>delegates content access control and redaction to a {@linkplain #modulator()} wrapper;</li>
 *
 * <li>delegates content pre/post-processing and housekeeping to a {@linkplain #processor()} wrapper;</li>
 *
 * <li>provides wrapper factories for handling {@linkplain #query(boolean) query} strings, splitting {@linkplain
 * #container() container} and {@linkplain #resource() resource} shape components and {@linkplain #trace(Collection)
 * tracing} RDF payloads.</li>
 *
 * </ul>
 *
 * @param <T> the self-bounded message type supporting fluent setters
 *
 * @see <a href="https://www.w3.org/TR/ldp/#ldprs">Linked Data Platform 1.0 - §4.3 RDF Source</a>
 */
public abstract class Actor<T extends Actor<T>> extends Delegator {

	private final Trace trace=tool(Trace.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Modulator modulator=new Modulator();
	private final Processor processor=new Processor();


	@SuppressWarnings("unchecked") private T self() { return (T)this; }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the permitted roles.
	 *
	 * @param roles the user {@linkplain Request#roles(Value...) roles} enabled to perform the resource action managed
	 *              by this actor; empty for public access; may be further restricted by role-based annotations in the
	 *              {@linkplain Request#shape() request shape}, if one is present
	 *
	 * @return this actor
	 *
	 * @throws NullPointerException if either {@code roles} is null or contains null values
	 * @see Modulator#role(Value...)
	 */
	public T role(final Value... roles) {
		return role(asList(roles));
	}

	/**
	 * Configures the permitted roles.
	 *
	 * @param roles the user {@linkplain Request#roles(Value...) roles} enabled to perform the resource action managed
	 *              by this actor; empty for public access; may be further restricted by role-based annotations in the
	 *              {@linkplain Request#shape() request shape}, if one is present
	 *
	 * @return this actor
	 *
	 * @throws NullPointerException if either {@code roles} is null or contains null values
	 * @see Modulator#role(Collection)
	 */
	public T role(final Collection<? extends Value> roles) {

		if ( roles == null ) {
			throw new NullPointerException("null roles");
		}

		if ( roles.contains(null) ) {
			throw new NullPointerException("null role");
		}

		modulator.role(roles);

		return self();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected Modulator modulator() { return modulator; }

	protected Processor processor() { return processor; }


	protected Wrapper query(final boolean accepted) {
		return handler -> request -> accepted || request.query().isEmpty() ? handler.handle(request)
				: request.reply(new Failure().status(Response.BadRequest).cause("unexpected query parameters"));
	}

	protected Wrapper container() {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	protected Wrapper resource() {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}


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
