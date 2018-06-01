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

package com.metreeca.link.handlers.ldp;

import com.metreeca.link._junk.*;
import com.metreeca.link._junk._Dispatcher;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.*;
import java.util.function.BiConsumer;

import static com.metreeca.spec.things.Maps.entry;
import static com.metreeca.spec.things.Maps.map;
import static com.metreeca.spec.things.Values.iri;

import static java.util.Collections.singleton;


public final class _Description implements _Handler { // !!! optimize for SPARQL

	private final Graph graph;

	private final _Handler dispatcher=new _Dispatcher(map(entry(_Request.GET, this::get)
	));


	public _Description(final Tool.Loader tools) {

		if ( tools == null ) {
			throw new NullPointerException("null tools");
		}

		final Setup setup=tools.get(Setup.Tool);

		this.graph=tools.get(Graph.Tool);
	}


	@Override public void handle(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		dispatcher.handle(tools, request, response

						.addHeader("Link", "<http://www.w3.org/ns/ldp#RDFResource>; rel=\"type\"")
						.addHeader("Link", "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\""),

				sink);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void get(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		final IRI target=iri(request.getTarget());
		final Collection<Statement> cell=new LinkedHashModel();

		final Queue<Value> pending=new ArrayDeque<>(singleton(target));
		final Collection<Value> visited=new HashSet<>();

		request.map(graph).browse(connection -> {

			while ( !pending.isEmpty() ) {

				final Value value=pending.poll();

				if ( visited.add(value) ) {
					if ( value.equals(target) || value instanceof BNode ) {

						try (final RepositoryResult<Statement> statements
								     =connection.getStatements((org.eclipse.rdf4j.model.Resource)value, null, null, true)) {
							while ( statements.hasNext() ) {

								final Statement statement=statements.next();

								cell.add(statement);
								pending.add(statement.getObject());
							}
						}

						try (final RepositoryResult<Statement> statements
								     =connection.getStatements(null, null, value, true)) {
							while ( statements.hasNext() ) {

								final Statement statement=statements.next();

								cell.add(statement);
								pending.add(statement.getSubject());
							}
						}

					} else if ( value instanceof IRI ) {

						try (final RepositoryResult<Statement> statements
								     =connection.getStatements((Resource)value, RDFS.LABEL, null, true)) {
							while ( statements.hasNext() ) { cell.add(statements.next()); }
						}

						try (final RepositoryResult<Statement> statements
								     =connection.getStatements((Resource)value, RDFS.COMMENT, null, true)) {
							while ( statements.hasNext() ) { cell.add(statements.next()); }
						}

					}

				}

			}

			return this;

		});

		new _Transfer(request, response).model(cell);

		sink.accept(request, response);

	}

}
