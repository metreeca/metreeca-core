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

package com.metreeca.next.handlers;


import com.metreeca.link.*;

import static com.metreeca.tray.Tray.tool;


/**
 * Path-based request router.
 */
public final class Router implements Handler {

	public static Router router() { return new Router(); }


	private final Index index=tool(Index.Tool);


	private Router() {}


	@Override public void handle(final Request request, final Response response) {
		index.lookup(request.path()).ifPresent(handler -> handler.handle(request, response));
	}

}