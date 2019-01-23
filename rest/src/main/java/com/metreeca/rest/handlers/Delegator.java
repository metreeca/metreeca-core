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

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Responder;


/**
 * Delegating handler.
 *
 * <p>Delegates request processing to a {@linkplain #delegate(Handler) delegate} handler, usually assembled as a
 * combination of other handlers and wrappers.</p>
 */
public abstract class Delegator implements Handler {

	private Handler container=request -> request.reply(response -> response);
	private Handler resource=request -> request.reply(response -> response);


	/**
	 * Configures the delegate handler.
	 *
	 * <p>Equivalent to {@code delegate(delegate, delegate)}.</p>
	 *
	 * @param delegate the handler request processing is delegated to
	 *
	 * @return this delegator
	 *
	 * @throws NullPointerException if {@code delegate} is null
	 */
	protected Delegator delegate(final Handler delegate) {

		if ( delegate == null ) {
			throw new NullPointerException("null delegate");
		}

		return delegate(delegate, delegate);
	}

	/**
	 * Configures container/resource delegate handlers.
	 *
	 * @param container the handler request processing is delegated to if the focus {@linkplain Request#item() item} of
	 *                  the request is a {@linkplain Request#container() container}
	 * @param resource  the handler request processing is delegated to if the focus {@linkplain Request#item() item} of
	 *                  the request is a plain resource
	 *
	 * @return this delegator
	 *
	 * @throws NullPointerException if either {@code container} or {@code resource} is null
	 */
	protected Delegator delegate(final Handler container, final Handler resource) {

		if ( container == null ) {
			throw new NullPointerException("null container handler");
		}

		if ( resource == null ) {
			throw new NullPointerException("null resource handler");
		}

		this.container=container;
		this.resource=resource;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Responder handle(final Request request) {
		return (request.container() ? container : resource).handle(request);
	}

}
