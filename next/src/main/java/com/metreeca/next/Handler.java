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

import java.util.function.UnaryOperator;


/**
 * Linked data resource handler.
 *
 * <p>Exposes and manages the state of a linked data resource, generating {@linkplain Response responses} in reaction to
 * {@linkplain Request requests}.</p>
 */
@FunctionalInterface public interface Handler {

	/**
	 * Handles a request/response exchange.
	 *
	 * @param request the inbound request for the managed linked data resource
	 *
	 * @return a lazy outbound response generated for the managed linked data resource in reaction to {@code request}
	 */
	public Lazy<Response> handle(final Request request);


	public default Handler before(final UnaryOperator<Request> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		return request -> handle(filter.apply(request));
	}

	public default Handler after(final UnaryOperator<Response> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		return request -> handle(request).map(filter);
	}

}
