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

package com.metreeca.link.handlers;

import com.metreeca.link._junk._Handler;
import com.metreeca.link._junk._Request;
import com.metreeca.link._junk._Response;
import com.metreeca.tray.Tool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import static java.util.Collections.unmodifiableMap;


public class _Dispatcher implements _Handler {

	private final Map<String, _Handler> handlers;
	private final _Handler fallback;


	public _Dispatcher(final Map<String, _Handler> handlers) {

		if ( handlers == null ) {
			throw new NullPointerException("null com.metreeca.next.handlers");
		}

		if ( handlers.containsKey(null) ) {
			throw new NullPointerException("null method");
		}

		if ( handlers.containsValue(null) ) {
			throw new NullPointerException("null handler");
		}

		// !!! provide default implementation for OPTIONS (see Handler.unsupported())

		this.handlers=new LinkedHashMap<>(handlers);
		this.fallback=handlers.getOrDefault(_Request.ANY, _Handler::unimplemented);
	}


	public Map<String, _Handler> getHandlers() {
		return unmodifiableMap(handlers);
	}


	@Override public void handle(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		Optional.ofNullable(handlers.get(request.getMethod()))
				.orElse(fallback)
				.handle(tools, request, response, sink);

	}

}
