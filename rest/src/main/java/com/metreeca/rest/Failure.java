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

package com.metreeca.rest;

import com.metreeca.rest.formats.JSONFormat;

import java.util.Optional;
import java.util.function.Function;

import javax.json.*;

import static com.metreeca.rest.formats.JSONFormat.json;


/**
 * HTTP processing failure.
 *
 * <p>Reports an error condition in an HTTP request processing operation; can be {@linkplain #apply(Response)
 * transferred} to the {@linkplain JSONFormat JSON} body of an HTTP response like:</p>
 *
 * <pre>{@code
 * <status>
 * Content-Type: application/json
 *
 * {
 *     "error": "<error>",  # a optional machine readable error type tag
 *     "cause": "<cause>",  # a optional a human readable error description
 *     "trace": <trace>     # a optional structured JSON error report
 * }
 * }</pre>
 */
public final class Failure implements Function<Response, Response> {

	/**
	 * The machine readable error tag for failures due to malformed data in message body.
	 */
	public static final String BodyMalformed="body-malformed";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int status=Response.BadRequest;

	private String error;
	private String label; // human readable cause label
	private Throwable cause; // machine readable cause
	private JsonValue trace;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the response status.
	 *
	 * @param status the HTTP status code associated with the error condition defined by this failure
	 *
	 * @return this failure
	 *
	 * @throws IllegalArgumentException if {@code status } is less than 0 or greater than 599
	 */
	public Failure status(final int status) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		this.status=status;

		return this;
	}

	/**
	 * Configures the error type.
	 *
	 * @param error a machine readable tag for the error condition defined by this failure
	 *
	 * @return this failure
	 *
	 * @throws NullPointerException if {@code error} is null
	 */
	public Failure error(final String error) {

		if ( error == null ) {
			throw new NullPointerException("null error");
		}

		this.error=error;

		return this;
	}

	/**
	 * Configures the error cause description.
	 *
	 * @param cause a human readable description of the error condition defined by this failure
	 *
	 * @return this failure
	 *
	 * @throws NullPointerException if {@code cause} is null
	 */
	public Failure cause(final String cause) {

		if ( cause == null ) {
			throw new NullPointerException("null cause");
		}

		this.label=cause;

		return this;
	}

	/**
	 * Configures the error cause.
	 *
	 * @param cause the underlying throwable that caused the error condition defined by this failure
	 *
	 * @return this failure
	 *
	 * @throws NullPointerException if {@code cause} is null
	 */
	public Failure cause(final Throwable cause) {

		if ( cause == null ) {
			throw new NullPointerException("null cause");
		}

		this.cause=cause;

		return this;
	}

	/**
	 * Configures the error trace.
	 *
	 * @param trace a structured JSON report describing the error condition defined by this failure
	 *
	 * @return this failure
	 *
	 * @throws NullPointerException if {@code trace} is null
	 */
	public Failure trace(final JsonValue trace) {

		if ( trace == null ) {
			throw new NullPointerException("null trace");
		}

		this.trace=trace;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Transfers the description of the error condition to an HTTP response.
	 *
	 * @param response the response to be updated
	 *
	 * @return the update {@code response}
	 *
	 * @throws NullPointerException if {@code response} is null
	 */
	@Override public Response apply(final Response response) {

		if ( response == null ) {
			throw new NullPointerException("null response");
		}

		return response
				.status(status)
				.cause(cause)
				.body(json(), ticket());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String toString() {
		return String.format("%d %s", status, ticket());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonObject ticket() {

		final JsonObjectBuilder builder=Json.createObjectBuilder();

		if ( error != null ) {
			builder.add("error", error);
		}

		if ( label != null ) {
			builder.add("cause", label);
		} else if ( cause != null ) {
			builder.add("cause", Optional.ofNullable(cause.getMessage()).orElseGet(cause::toString));
		}

		if ( trace != null ) {
			builder.add("trace", trace);
		}

		return builder.build();
	}

}
