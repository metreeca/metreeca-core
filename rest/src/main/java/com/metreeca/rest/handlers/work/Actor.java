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

package com.metreeca.rest.handlers.work;

import com.metreeca.rest.*;


/**
 * Resource actor.
 *
 * <p>Handles actions on linked data resources.</p>
 *
 * <p>The abstract base class:</p>
 *
 * <ul>
 *
 * <li>!!!</li>
 *
 * </ul>
 */
public abstract class Actor implements Wrapper, Handler {

	protected static Wrapper query(final boolean accepted) {
		return handler -> request -> accepted || request.query().isEmpty() ? handler.handle(request)
				: request.reply(new Failure().status(Response.BadRequest).cause("unexpected query parameters"));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	private final Wrapper wrapper;
	private final Handler handler;

	private final Handler delegate;


	protected Actor(final Wrapper wrapper, final Handler handler) {

		if ( wrapper == null ) {
			throw new NullPointerException("null wrapper");
		}

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		this.wrapper=wrapper;
		this.handler=handler;

		this.delegate=wrapper.wrap(handler);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return wrapper.wrap(handler);
	}

	@Override public Wrapper wrap(final Wrapper wrapper) {
		return new Actor(wrapper.wrap(wrapper), handler) {};
	}


	@Override public Responder handle(final Request request) {
		return delegate.handle(request);
	}

}
