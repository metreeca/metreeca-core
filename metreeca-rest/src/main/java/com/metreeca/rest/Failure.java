/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

import com.metreeca.rest.formats.JSONFormat;
import com.metreeca.tree.Trace;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
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
 *     "error": "<error>",  # optional machine readable error tag
 *     "notes": "<notes>",  # optional human readable error notes
 *     "trace": <trace>     # optional structured JSON error trace
 * }
 * }</pre>
 */
public final class Failure implements Function<Response, Response> {

	/**
	 * The machine readable error tag for failures due to malformed message body.
	 */
	public static final String BodyMalformed="body-malformed";

	/**
	 * The machine readable error tag for failures due to invalid data.
	 */
	public static final String DataInvalid="data-invalid";


	/**
	 * Creates a failure for a request authorization issue.
	 *
	 * @return a new failure reporting {@code cause} with a {@value Response#Unauthorized} status code
	 */
	public static Failure unauthorized() {
		return new Failure()
				.status(Response.Unauthorized);
	}

	/**
	 * Creates a failure for a payload parsing issue.
	 *
	 * @param cause the parsing error
	 *
	 * @return a new failure reporting {@code cause} with a {@value Response#BadRequest} status code
	 *
	 * @throws NullPointerException if {@code cause} is null
	 */
	public static Failure malformed(final Throwable cause) {

		if ( cause == null ) {
			throw new NullPointerException("null cause");
		}

		return new Failure()
				.status(Response.BadRequest)
				.error(BodyMalformed)
				.notes(Optional.ofNullable(cause.getMessage()).orElse(""))
				.cause(cause);
	}

	/**
	 * Creates a failure for a payload validation issue.
	 *
	 * @param trace the validation trace
	 *
	 * @return a new failure reporting {@code trace} with a {@value Response#UnprocessableEntity} status code
	 *
	 * @throws NullPointerException if {@code trace} is null
	 */
	public static Failure invalid(final Trace trace) {

		if ( trace == null ) {
			throw new NullPointerException("null trace");
		}

		return new Failure()
				.status(Response.UnprocessableEntity)
				.error(DataInvalid)
				.trace(trace);
	}

	/**
	 * Creates a failure for an internal server error.
	 *
	 * @param cause the internal server error
	 *
	 * @return a new failure reporting {@code cause} with a {@value Response#InternalServerError} status code; no detail
	 * about {@code cause} is disclosed to the client
	 *
	 * @throws NullPointerException if {@code cause} is null
	 */
	public static Failure internal(final Throwable cause) {

		if ( cause == null ) {
			throw new NullPointerException("null cause");
		}

		return new Failure()
				.status(Response.InternalServerError)
				.notes("unable to process request: see server logs for details")
				.cause(cause);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int status=Response.BadRequest;

	private String error;
	private String notes;

	private JsonValue trace;
	private Throwable cause;

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the response status.
	 *
	 * @param status the HTTP status code associated with the error condition defined by this failure
	 *
	 * @return this failure
	 *
	 * @throws IllegalArgumentException if {@code status } is less than 100 or greater than 599
	 */
	public Failure status(final int status) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		this.status=status;

		return this;
	}

	/**
	 * Configures the error tag.
	 *
	 * @param error a machine-readable tag for the error condition defined by this failure; ignored if null or empty
	 *
	 * @return this failure
	 */
	public Failure error(final String error) {

		this.error=(error == null || error.isEmpty()) ? null : error;

		return this;
	}

	/**
	 * Configures the error notes.
	 *
	 * @param notes a human readable description of the error condition defined by this failure; ignored if null or
	 *              empty
	 *
	 * @return this failure
	 */
	public Failure notes(final String notes) {

		this.notes=(notes == null || notes.isEmpty()) ? null : notes;

		return this;
	}

	/**
	 * Configures error trace.
	 *
	 * @param trace a shape validation trace describing the error condition defined by this failure; ignored if null or
	 *              empty
	 *
	 * @return this failure
	 *
	 * @throws NullPointerException if {@code trace} is null
	 */
	public Failure trace(final Trace trace) {
		return trace(trace == null || trace.isEmpty() ? null : format(trace));
	}

	/**
	 * Configures error trace.
	 *
	 * @param trace a structured JSON report describing the error condition defined by this failure; ignored if null or
	 *              empty
	 *
	 * @return this failure
	 */
	public Failure trace(final JsonValue trace) {

		this.trace=trace == null
				|| trace.equals(JsonValue.NULL)
				|| trace.equals(JsonValue.EMPTY_JSON_OBJECT)
				|| trace.equals(JsonValue.EMPTY_JSON_ARRAY)
				|| trace.equals(Json.createValue(""))

				? null : trace;

		return this;
	}


	/**
	 * Configures the error cause.
	 *
	 * @param cause a human readable description of cause of the error condition defined by this failure; ignored if
	 *              null or empty
	 *
	 * @return this failure
	 */
	public Failure cause(final String cause) {
		return cause(cause == null || cause.isEmpty() ? null : new RuntimeException(cause));
	}

	/**
	 * Configures the error cause.
	 *
	 * @param cause the underlying throwable that caused the error condition defined by this failure; ignored if null
	 *
	 * @return this failure
	 */
	public Failure cause(final Throwable cause) {

		this.cause=cause;

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


	@Override public String toString() {

		final JsonObjectBuilder builder=Json.createObjectBuilder();

		if ( error != null ) {
			builder.add("error", error);
		}

		if ( notes != null ) {
			builder.add("notes", notes);
		}

		if ( trace != null ) {
			builder.add("trace", trace);
		}

		if ( cause != null ) {
			builder.add("cause", cause.getMessage());
		}

		return status+" "+builder.build();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonObject ticket() {

		final JsonObjectBuilder builder=Json.createObjectBuilder();

		if ( error != null ) {
			builder.add("error", error);
		}

		if ( notes != null ) {
			builder.add("notes", notes);
		}

		if ( trace != null ) {
			builder.add("trace", trace);
		}

		return builder.build();
	}


	private JsonObject format(final Trace trace) {

		final JsonObjectBuilder builder=Json.createObjectBuilder();

		final Map<String, Collection<Object>> issues=trace.getIssues();

		if ( !issues.isEmpty() ) {

			final JsonObjectBuilder errors=Json.createObjectBuilder();

			issues.forEach((detail, values) -> {

				final JsonArrayBuilder objects=Json.createArrayBuilder();

				values.forEach(value -> {
					if ( value == null ) {

						objects.add(JsonValue.NULL);

					} else if ( value instanceof Boolean ) {

						objects.add((Boolean)value);

					} else if ( value instanceof BigDecimal ) {

						objects.add((BigDecimal)value);

					} else if ( value instanceof BigInteger ) {

						objects.add((BigInteger)value);

					} else if ( value instanceof Double || value instanceof Float ) {

						objects.add(((Number)value).doubleValue());

					} else if ( value instanceof Number ) {

						objects.add(((Number)value).longValue());

					} else {

						objects.add(value.toString());

					}
				});

				errors.add(detail, objects.build());

			});

			builder.add("", errors);
		}

		trace.getFields().forEach((name, nested) -> {

			if ( !nested.isEmpty() ) {
				builder.add(name.toString(), format(nested));
			}

		});

		return builder.build();
	}

}
