
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

package com.metreeca.gate.wrappers;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Wrapper;


/**
 * App launcher.
 *
 * <p>Delegates {@linkplain Request#interactive() interactive} {@linkplain Request#GET} requests to an {@linkplain
 * #Launcher(Handler) alternate} handler.</p>
 */
public final class Launcher implements Wrapper {

	private final Handler interactive;


	/**
	 * Creates an app launcher.
	 *
	 * @param interactive the alternate handler for {@linkplain Request#interactive() interactive} requests
	 *
	 * @throws NullPointerException if {@code handler} is null
	 */
	public Launcher(final Handler interactive) {

		if ( interactive == null ) {
			throw new NullPointerException("null handler");
		}

		this.interactive=interactive;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) { // delegate only resources actually managed by handler
		return request -> handler.handle(request).flatMap(response ->
				response.status() > 0 && request.method().equals(Request.GET) && request.interactive() ?
						interactive.handle(request) : consumer -> consumer.accept(response)
		);
	}

}
