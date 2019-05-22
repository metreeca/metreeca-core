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

import com.metreeca.rest.Handler;
import com.metreeca.rest.Wrapper;


/**
 * XSS protector.
 *
 * <p>Configures the following standard XSS protection headers (unless already defined by wrapper handlers):</p>
 *
 * <ul>
 *
 * <li><a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP">Content Security Policy (CSP)</a>: {@value
 * ContentSecurityPolicy}</li>
 *
 * <li><a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Strict-Transport-Security">Strict-Transport-Security</a>:
 * {@value StrictTransportSecurity}</li>
 *
 * <li><a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-XSS-Protection">X-XSS-Protection</a>:
 * {@value XXSSProtection}</li>
 *
 * </ul>
 */
public final class Protector implements Wrapper {

	private static final String ContentSecurityPolicy="default-src 'self'; base-uri 'self'";
	private static final String StrictTransportSecurity="max-age=86400";
	private static final String XXSSProtection="1; mode=block";


	@Override public Handler wrap(final Handler handler) {
		return request -> handler.handle(request).map(response -> response
				.header("~Content-Security-Policy", ContentSecurityPolicy)
				.header("~Strict-Transport-Security", StrictTransportSecurity)
				.header("~X-XSS-Protection", XXSSProtection)
		);
	}

}
