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

package com.metreeca.next;

import java.util.function.Consumer;


/**
 * HTTP response.
 */
public final class Response extends Outbound<Response> implements Lazy<Response> {

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
	public static final int UnprocessableEntity=422; // https://tools.ietf.org/html/rfc4918#section-11.2

	public static final int InternalServerError=500; // https://tools.ietf.org/html/rfc7231#section-6.6.1
	public static final int NotImplemented=501; // https://tools.ietf.org/html/rfc7231#section-6.6.2
	public static final int BadGateway=502; // https://tools.ietf.org/html/rfc7231#section-6.6.3
	public static final int ServiceUnavailable=503; // https://tools.ietf.org/html/rfc7231#section-6.6.4
	public static final int GatewayTimeout=504; // https://tools.ietf.org/html/rfc7231#section-6.6.5


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Request request;

	private int status;


	public Response(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		this.request=request;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override protected Response self() {
		return this;
	}

	@Override public void accept(final Consumer<Response> consumer) {

		if ( consumer == null ) {
			throw new NullPointerException("null consumer");
		}

		consumer.accept(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Request request() {
		return request;
	}


	public int status() {
		return status;
	}

	public Response status(final int status) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status ["+status+"]");
		}

		this.status=status;

		return this;
	}

}
