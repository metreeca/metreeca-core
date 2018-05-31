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

package com.metreeca.link.wrappers;


import com.metreeca.link.Handler;
import com.metreeca.link.Wrapper;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import static com.metreeca.link._work.Binder.binder;
import static com.metreeca.tray.Tray.tool;


/**
 * SPARQL Update post-processor.
 *
 * <p>Executes a SPARQL Update post-processing script in the shared {@linkplain Graph#Tool graph} tool on successful
 * request processing by the wrapped handler.</p>
 */
public final class Processor implements Wrapper {

	public static Processor processor() { return new Processor(); }


	private final Graph graph=tool(Graph.Tool);

	private String script;


	private Processor() {}


	/**
	 * Configures the SPARQL Update script.
	 *
	 * @param script the SPARQL Update script to be executed by this processor on successful request processing; empty
	 *               scripts are ignored
	 *
	 * @return this processor
	 */
	public Processor script(final String script) {

		if ( script == null ) {
			throw new NullPointerException("null script update script");
		}

		this.script=script;

		return this;
	}


	@Override public Handler wrap(final Handler handler) {
		return (request, response) -> {
			if ( script.isEmpty() ) { handler.handle(request, response); } else {
				handler.handle(

						writer ->

								writer.copy(request).done(),

						reader -> {

							if ( reader.success() ) {
								try (final RepositoryConnection connection=graph.connect()) {
									binder()

											.time()
											.user(reader.request().user())
											.focus(reader.focus())

											.bind(connection.prepareUpdate(QueryLanguage.SPARQL, script, request.base()))

											.execute();
								}
							}

							response.copy(reader).done();

						}

				);
			}
		};
	}

}
