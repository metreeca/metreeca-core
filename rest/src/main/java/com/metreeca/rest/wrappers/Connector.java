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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;
import com.metreeca.tray.rdf.Graph;

import static com.metreeca.tray.Tray.tool;


/**
 * Graph connection manager.
 *
 * <p>Executes wrapped handlers inside a shared connection to the the system {@linkplain Graph#Factory graph
 * database}.</p>
 *
 * <p>If the incoming request is not {@linkplain Request#safe() safe}, wrapped handlers are executed inside a single
 * transaction on the shared connection, which is automatically committed on {@linkplain Response#success() successful}
 * response or rolled back otherwise.</p>
 */
public final class Connector implements Wrapper {

	private final Graph graph=tool(Graph.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return request -> consumer -> {
			if ( request.safe() ) {

				graph.query(connection -> {

					handler.handle(request).accept(consumer);

				});

			} else {

				graph.update(connection -> {
					handler.handle(request).map(response -> {

						if ( !response.success() && connection.isActive() ) {
							connection.rollback();
						}

						return response;

					}).accept(consumer);
				});

			}
		};
	}

}
