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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Wrapper;
import com.metreeca.tray.sys._Setup;
import com.metreeca.tray.sys.Trace;

import static com.metreeca.tray._Tray.tool;


/**
 * CORS request manager (work in progress…).
 *
 * <p>Manages CORS HTTP requests.</p>
 *
 * @see <a href="https://fetch.spec.whatwg.org/#cors-protocol">Fetch - § 3.2 CORS protocol</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS">Cross-Origin Resource Sharing (CORS) @ MDN</a>
 */
public final class CORS implements Wrapper {

	// !!! https://www.html5rocks.com/static/images/cors_server_flowchart.png

	public static CORS cors() { return new CORS(); }


	private final _Setup setup=tool(_Setup.Factory);
	private final Trace trace=tool(Trace.Factory);

	private final boolean enabled=setup.get("cors", false);


	private CORS() {}


	@Override public Handler wrap(final Handler handler) {

		trace.info(this, enabled ? "enabled" : "disabled");

		return enabled ? (request, response) -> handler.handle(

				writer -> writer.copy(request).done(),

				reader -> response.copy(reader)
						.header("Access-Control-Allow-Origin", request.header("Origin").orElse(""))
						.done()

		) : handler;
	}
}
