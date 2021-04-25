/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest;

import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.json.JsonObject;

import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.formats.TextFormat.text;

import static java.lang.String.format;

/**
 * Message exception.
 *
 * <p>Thrown to report message processing issues.</p>
 */
public final class MessageException extends RuntimeException implements Handler, UnaryOperator<Response> {

	private static final long serialVersionUID=6385340424276867964L;


	/**
	 * Creates a no-op response generator.
	 *
	 * @return a no-op response generator
	 */
	public static MessageException status() {
		return new MessageException();
	}

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
	 * @param details the human readable response details; the {@code {@\}} placeholder is replaced with the focus
	 *                {@linkplain Request#item() item} IRI of the originating request
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


	private static boolean redirect(final int status) {
		return status == 201 || status >= 301 && status <= 303 || status >= 307 && status <= 308;
	}

	private static String fill(final String details, final Response response) {
		return details.replace("{@}", response.request().item());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int status;

	private final UnaryOperator<Response> report;


	private MessageException() {

		super(format("%3d", 0));

		this.status=0;
		this.report=response -> response;
	}

	private MessageException(final int status) {

		super(format("%3d", status));

		this.status=status;
		this.report=response -> response.status(status);
	}

	private MessageException(final int status, final String details) {

		super(format("%3d %s", status, details));

		this.status=status;
		this.report=response
				-> redirect(status) ? response.status(status).header("Location", fill(details, response))
				: status < 500 ? response.status(status).body(text(), details)
				: response.status(status).cause(this);
	}

	private MessageException(final int status, final JsonObject details) {

		super(format("%3d %s", status, details));

		this.status=status;
		this.report=response -> status < 500
				? response.status(status).body(json(), details)
				: response.status(status).cause(this);
	}

	private MessageException(final int status, final Throwable cause) {

		super(format("%3d %s", status, cause), cause);

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

	@Override public Future<Response> handle(final Request request) {
		return request.reply(report);
	}

	@Override public Response apply(final Response response) {
		return report.apply(response);
	}

}
