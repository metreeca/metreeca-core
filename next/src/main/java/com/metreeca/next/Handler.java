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

import com.metreeca.form.Form;


/**
 * Resource handler.
 *
 * <p>Exposes and manages the state of a linked data resource, generating {@linkplain Response responses} in reaction
 * to {@linkplain Request requests}.</p>
 */
@FunctionalInterface public interface Handler {

	/**
	 * Handles refused requests.
	 *
	 * @param request the incoming request
	 *
	 * @return a responder providing an empty response with a {@value Response#Unauthorized} status code, if the request
	 * {@linkplain Request#user()  user} is anonymous (that it's equal to {@link Form#none}), or a {@value
	 * Response#Forbidden} status code, otherwise
	 *
	 * @throws NullPointerException if {@code request} is {@code null}
	 */
	public static Responder refused(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return request.user().equals(Form.none) ? unauthorized(request) : forbidden(request);
	}

	/**
	 * Handles unauthorized requests.
	 *
	 * @param request the incoming request
	 *
	 * @return a responder providing an empty response with a {@value Response#Unauthorized} status {@code}
	 *
	 * @throws NullPointerException if {@code request} is {@code null}
	 */
	public static Responder unauthorized(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return request.reply(response -> response.status(Response.Unauthorized)); // WWW-Authenticate set by wrappers
	}

	/**
	 * Handles forbidden requests.
	 *
	 * @param request the incoming request
	 *
	 * @return a responder providing an empty response with a {@value Response#Forbidden} status code
	 *
	 * @throws NullPointerException if {@code request} is {@code null}
	 */
	public static Responder forbidden(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return request.reply(response -> response.status(Response.Forbidden));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles a request/response exchange.
	 *
	 * @param request the inbound request for the managed linked data resource
	 *
	 * @return a responder providing a response generated for the managed linked data resource in reaction to {@code
	 * request}
	 */
	public Responder handle(final Request request);

}
