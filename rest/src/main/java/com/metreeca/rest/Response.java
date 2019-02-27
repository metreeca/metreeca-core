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

import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;


/**
 * HTTP response.
 *
 * <p>Handles state/behaviour for HTTP responses.</p>
 */
public final class Response extends Message<Response> {

	public static final int OK=200; // https://tools.ietf.org/html/rfc7231#section-6.3.1
	public static final int Created=201; // https://tools.ietf.org/html/rfc7231#section-6.3.2
	public static final int Accepted=202; // https://tools.ietf.org/html/rfc7231#section-6.3.3
	public static final int NoContent=204; // https://tools.ietf.org/html/rfc7231#section-6.3.5

	public static final int MovedPermanently=301; // https://tools.ietf.org/html/rfc7231#section-6.4.2
	public static final int SeeOther=303; // https://tools.ietf.org/html/rfc7231#section-6.4.4

	public static final int BadRequest=400; // https://tools.ietf.org/html/rfc7231#section-6.5.1
	public static final int Unauthorized=401; // https://tools.ietf.org/html/rfc7235#section-3.1
	public static final int Forbidden=403; // https://tools.ietf.org/html/rfc7231#section-6.5.3
	public static final int NotFound=404; // https://tools.ietf.org/html/rfc7231#section-6.5.4
	public static final int MethodNotAllowed=405; // https://tools.ietf.org/html/rfc7231#section-6.5.5
	public static final int Conflict=409; // https://tools.ietf.org/html/rfc7231#section-6.5.8
	public static final int PayloadTooLarge=413; // https://tools.ietf.org/html/rfc7231#section-6.5.11
	public static final int UnsupportedMediaType=415; // https://tools.ietf.org/html/rfc7231#section-6.5.13
	public static final int UnprocessableEntity=422; // https://tools.ietf.org/html/rfc4918#section-11.2

	public static final int InternalServerError=500; // https://tools.ietf.org/html/rfc7231#section-6.6.1
	public static final int NotImplemented=501; // https://tools.ietf.org/html/rfc7231#section-6.6.2
	public static final int BadGateway=502; // https://tools.ietf.org/html/rfc7231#section-6.6.3
	public static final int ServiceUnavailable=503; // https://tools.ietf.org/html/rfc7231#section-6.6.4
	public static final int GatewayTimeout=504; // https://tools.ietf.org/html/rfc7231#section-6.6.5


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Request request; // the originating request

	private int status; // the HTTP status code
	private Throwable cause; // a (possibly null) optional cause for an error status code


	/**
	 * Creates a new response for a request.
	 *
	 * @param request the originating request for the new response
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Response(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		this.request=request;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the focus item IRI of this response.
	 *
	 * @return the absolute IRI included in the {@code Location} header of this response, if defined; the {@linkplain
	 * Request#item() focus item} IRI of the originating request otherwise
	 */
	@Override public IRI item() {
		return header("location")
				.map(Values::iri)
				.orElseGet(() -> request().item());
	}

	/**
	 * Retrieves the originating request for this response.
	 *
	 * @return the originating request for this response
	 */
	@Override public Request request() {
		return request;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks if this response is successful.
	 *
	 * @return {@code true} if the {@linkplain #status() status} code is in the {@code 2XX} range; {@code false}
	 * otherwise
	 */
	public boolean success() {
		return status/100 == 2;
	}

	/**
	 * Checks if this response is an error.
	 *
	 * @return {@code true} if the {@linkplain #status() status} code is in beyond the {@code 3XX} range; {@code false}
	 * otherwise
	 */
	public boolean error() {
		return status/100 > 3;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the status code of this response.
	 *
	 * @return the status code of this response
	 */
	public int status() {
		return status;
	}

	/**
	 * Configures the status code of this response.
	 *
	 * @param status the status code of this response
	 *
	 * @return this response
	 *
	 * @throws IllegalArgumentException if {@code response } is less than 0 or greater than 599
	 */
	public Response status(final int status) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		this.status=status;

		return this;
	}


	/**
	 * Retrieves the cause for the status code.
	 *
	 * @return a optional throwable causing the selection of the status code
	 */
	public Optional<Throwable> cause() { return Optional.ofNullable(cause); }

	/**
	 * Configures the cause for the status code.
	 *
	 * @param cause the (possibly null) throwable causing the selection of the status code
	 *
	 * @return this response
	 */
	public Response cause(final Throwable cause) {

		this.cause=cause;

		return this;
	}

}
