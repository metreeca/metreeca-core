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

package com.metreeca.gate.wrappers;


import com.metreeca.rest.Handler;
import com.metreeca.rest.Wrapper;


/**
 * CORS request manager.
 *
 * <p>Manages CORS HTTP requests.</p>
 *
 * @see <a href="https://fetch.spec.whatwg.org/#cors-protocol">Fetch - § 3.2 CORS protocol</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS">Cross-Origin Resource Sharing (CORS) @ MDN</a>
 * @deprecated <strong>Don't use in production</strong> / Provisional implementation with unsafe shortcuts;
 */
@Deprecated final class CORS implements Wrapper {

	// !!! https://www.html5rocks.com/static/images/cors_server_flowchart.png

	@Override public Handler wrap(final Handler handler) {
		return request -> handler.handle(request).map(response -> response
				.header("Access-Control-Allow-Origin", request.header("Origin").orElse(""))); // !!! ;(
	}

}
