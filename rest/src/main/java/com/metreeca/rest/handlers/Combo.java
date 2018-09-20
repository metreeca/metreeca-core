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

package com.metreeca.rest.handlers;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Responder;


/**
 * Delegating handler.
 *
 * <p>Delegates request processing to a delegate handler, usually assembled as a combination of other handlers and
 * wrappers.</p>
 */
public abstract class Combo implements Handler {

	private final Handler delegate;


	/**
	 * Creates a combo handler.
	 *
	 * @param delegate the handler request processing is to be delegated to
	 *
	 * @throws NullPointerException if {@code delegate} is null
	 */
	protected Combo(final Handler delegate) {

		if ( delegate == null ) {
			throw new NullPointerException("null delegate");
		}

		this.delegate=delegate;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Responder handle(final Request request) {
		return delegate.handle(request);
	}

}
