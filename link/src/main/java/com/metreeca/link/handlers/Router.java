/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.link.handlers;

import com.metreeca.link.*;
import com.metreeca.tray.Tool;

import java.util.function.BiConsumer;


public final class Router implements Handler {

	public static Router router() {
		return new Router();
	}


	@Override public void handle(final Tool.Loader tools,
			final Request request, final Response response, final BiConsumer<Request, Response> sink) {

		if ( response.getStatus() != 0 ) { sink.accept(request, response); } else { // conditional processing

			final Index index=tools.get(Index.Tool); // !!! to constructor

			final String base=request.getBase();
			final String target=request.getTarget();
			final String path=target.substring(base.length()-1);

			final Handler handler=index.get(path);

			if ( handler != null ) { // matched: process with handler

				handler.handle(tools, request, response, sink);

			} else { // not matched: forward to the pipeline and let the server adapter eventually handle it

				sink.accept(request, response);

			}

		}
	}
}
