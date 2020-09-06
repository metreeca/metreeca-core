/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

import javax.json.JsonObject;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.formats.TextFormat.text;

/**
 * Message exception.
 *
 * <p>Thrown to report message processing issues.</p>
 */
public final class MessageException extends RuntimeException implements UnaryOperator<Response> {

	private static final long serialVersionUID=6385340424276867964L;


	/**
	 * Creates a shorthand response generator.
	 *
	 * @param status the status code for the response
	 *
	 * @return a shorthand response generator for {@code status}
	 *
	 * @throws IllegalArgumentException if {@code response } is less than 100 or greater than 599
	 */
	public static MessageException status(final int status) {

		if ( status < 100 || status > 599 ) { // 0 used internally
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		return new MessageException(status);
	}

	/**
	 * Creates a shorthand response generator.
	 *
	 * @param status  the response status code
	 * @param details the human readable response details
	 *
	 * @return a shorthand response generator for {@code status} and {@code details}
	 *
	 * @throws IllegalArgumentException if {@code response } is less than 100 or greater than 599
	 * @throws NullPointerException     if {@code details} is null
	 */
	public static MessageException status(final int status, final String details) {

		if ( status < 100 || status > 599 ) { // 0 used internally
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		if ( details == null ) {
			throw new NullPointerException("null details");
		}

		return new MessageException(status, details);
	}

	/**
	 * Creates a shorthand response generator.
	 *
	 * @param status  the response status code
	 * @param details the machine readable response details
	 *
	 * @return a shorthand response generator for {@code status} and {@code details}
	 *
	 * @throws IllegalArgumentException if {@code response } is less than 100 or greater than 599
	 * @throws NullPointerException     if {@code details} is null
	 */
	public static MessageException status(final int status, final JsonObject details) {

		if ( status < 100 || status > 599 ) { // 0 used internally
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		if ( details == null ) {
			throw new NullPointerException("null details");
		}

		return new MessageException(status, details);
	}

	/**
	 * Creates a shorthand response generator.
	 *
	 * @param status the response status code
	 * @param cause  the exceptional response cause
	 *
	 * @return a shorthand response generator for {@code status} and {@code cause}
	 *
	 * @throws IllegalArgumentException if {@code response } is less than 100 or greater than 599
	 * @throws NullPointerException     if {@code cause} is null
	 */
	public static MessageException status(final int status, final Throwable cause) {

		if ( status < 100 || status > 599 ) { // 0 used internally
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		if ( cause == null ) {
			throw new NullPointerException("null cause");
		}

		return new MessageException(status, cause);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int status;

	private final UnaryOperator<Response> report;


	private MessageException(final int status) {

		super(String.format("%3d", status));

		this.status=status;
		this.report=response -> response.status(status);
	}

	private MessageException(final int status, final String details) {

		super(String.format("%3d %s", status, details));

		this.status=status;
		this.report=response -> status < 500
				? response.status(status).body(text(), details)
				: response.status(status).cause(new Exception(details));
	}

	private MessageException(final int status, final JsonObject details) {

		super(String.format("%3d %s", status, details));

		this.status=status;
		this.report=response -> status < 500
				? response.status(status).body(json(), details)
				: response.status(status).cause(new Exception(details.toString()));
	}

	private MessageException(final int status, final Throwable cause) {

		super(String.format("%3d %s", status, cause), cause);

		final String message=Optional.ofNullable(cause.getMessage()).orElseGet(cause::toString);

		this.status=status;
		this.report=response -> status < 500
				? response.status(status).cause(cause).body(text(), message)
				: response.status(status).cause(cause);
	}


	/**
	 * Retrieves the status code of this message exception.
	 *
	 * @return the HTTP status code describing the root cause of this message exception
	 */
	public int getStatus() {
		return status;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Response apply(final Response response) {
		return report.apply(response);
	}

}
