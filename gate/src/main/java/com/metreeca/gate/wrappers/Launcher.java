
/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

import com.metreeca.rest.*;

import static java.lang.String.format;


/**
 * App launcher.
 *
 * <p>Delegates {@linkplain Request#interactive() interactive} requests to an {@linkplain #Launcher(Handler) alternate}
 * handler.</p>
 */
public final class Launcher implements Wrapper {

	private static final String PathCookie="metreeca";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Handler interactive;


	/**
	 * Creates an app launcher.
	 *
	 * @param path the root-relative path of the app to be launched; the root-relative {@linkplain Request#path() path}
	 *             of the original request is made available to the app  under the {@value PathCookie} session cookie
	 *
	 * @throws NullPointerException     if {@code path} is null
	 * @throws IllegalArgumentException if {@code path} does not start with a trailing slash
	 */
	public Launcher(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( !path.startsWith("/") ) {
			throw new IllegalArgumentException("illegal path ["+path+"]");
		}

		this.interactive=request -> request.reply(response -> request.path().equals(path) ? response : response
				.status(Response.SeeOther)
				.header("Location", path)
				.header("Set-Cookie", format("%s=%s; path=%s", PathCookie, request.path(), path))
		);
	}

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

	@Override public Handler wrap(final Handler handler) {
		return request -> (request.interactive() ? interactive : handler).handle(request);
	}

}
