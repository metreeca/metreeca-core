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

import com.metreeca.rest.*;


/**
 * Delegating handler.
 *
 * <p>Delegates request processing to a {@linkplain #delegate(Handler) delegate} handler, possibly assembled as a
 * combination of other handlers and wrappers.</p>
 */
public abstract class Delegator implements Handler {

	private Handler delegate;


	/**
	 * Configures the delegate handler.
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

		if ( this.delegate != null ) {
			throw new IllegalStateException("delegate already defined");
		}

		this.delegate=delegate;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler with(final Wrapper wrapper) {

		if ( delegate == null ) {
			throw new NullPointerException("undefined delegate");
		}

		return delegate.with(wrapper);
	}

	@Override public Responder handle(final Request request) {

		if ( delegate == null ) {
			throw new NullPointerException("undefined delegate");
		}

		return delegate.handle(request);
	}

}
