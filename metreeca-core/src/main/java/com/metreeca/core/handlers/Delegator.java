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

package com.metreeca.core.handlers;

import com.metreeca.core.*;

import static java.util.function.Function.identity;


/**
 * Delegating handler.
 *
 * <p>Delegates request processing to a {@linkplain #delegate(Handler) delegate} handler, possibly assembled as a
 * combination of other handlers and wrappers.</p>
 */
public abstract class Delegator implements Handler {

	private Handler delegate=request -> request.reply(identity());


	/**
	 * Configures the delegate handler.
	 *
	 * @param delegate the handler request processing is delegated to
	 *
	 * @return this delegator
	 *
	 * @throws NullPointerException     if {@code delegate} is null
	 */
	protected Delegator delegate(final Handler delegate) {

		if ( delegate == null ) {
			throw new NullPointerException("null delegate");
		}

		this.delegate=delegate;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Future<Response> handle(final Request request) {
		return delegate.handle(request);
	}

}
