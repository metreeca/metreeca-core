/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 *  Metreeca is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or(at your option) any later version.
 *
 *  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with Metreeca.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.wrappers;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Wrapper;


/**
 * Multipart message manager.
 *
 * <p>Manages multipart message bodies for incoming requests and outgoing responses.</p>
 *
 * @deprecated To be implemented
 */
@Deprecated final class Multipart implements Wrapper {

	// !!! see https://github.com/DanielN/multipart-handler/tree/master/src/main/java/com/github/danieln/multipart
	// !!! harden against malformed multipart messages (e.g. mismatched boundary in content-type header and body)

	private String main=""; // !!! rename


	/**
	 * Configures the main message part for multipart requests.
	 *
	 * @param main the name of the message part containing the main payload in multipart requests; empty if no main
	 *             payload is expected
	 *
	 * @return this wrapper
	 *
	 * @throws NullPointerException if {@code main} is null
	 */
	public Multipart main(final String main) {

		if ( main == null ) {
			throw new NullPointerException("null main");
		}

		this.main=main;

		return this;
	}


	@Override public Handler wrap(final Handler handler) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

}
