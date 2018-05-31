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

package com.metreeca.mill.tasks.rdf;


import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import static com.metreeca.mill._Cell.cell;
import static com.metreeca.spec.things.Values.bnode;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Trace.clip;


/**
 * SPARQL extraction/validation task.
 */
public class SPARQL implements Task {

	private final Trace trace=tool(Trace.Tool);


	private String query="";


	public SPARQL query(final String query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		this.query=query;

		return this;
	}

	public SPARQL severity(final Trace.Level level, final String message) {


		return this;
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) { // !!! refactor

		final Repository repository=new SailRepository(new MemoryStore()); // !!! or remote endpoint parametrized on item

		repository.initialize();

		// RepositoryConnection is not thread-safe (http://docs.rdf4j.org/programming/#_multithreaded_repository_access)

		final RepositoryConnection connection=repository.getConnection();

		return items.flatMap(item -> {

			final Resource feed=item.focus();
			final Resource sink=bnode();

			try {

				connection.add(item.model(), feed);

				final Query query=connection.prepareQuery(this.query);

				query.setBinding("feed", feed);
				query.setBinding("sink", sink);

				if ( query instanceof GraphQuery ) {

					final Collection<Statement> model=new ArrayList<>();

					((GraphQuery)query).evaluate(new AbstractRDFHandler() {
						@Override public void handleStatement(final Statement statement) { model.add(statement); }
					});

					return Stream.of(cell(sink, model));

				} else if ( query instanceof BooleanQuery ) { // !!! extract/validate modes

					if ( ((BooleanQuery)query).evaluate() ) {

						return Stream.of(item);

					} else {

						trace.error(this, String.format("<%s> / failed assertion: %s", clip(item.focus()), "zot"));

						return Stream.of(); // !!! minimum level? skip only if error?

					}

				} else {

					throw new UnsupportedOperationException("unsupported query type ["+query.getClass().getName()+"]");

				}

			} finally { connection.clear(feed); }

		}).onClose(() -> {

			connection.close();
			repository.shutDown();

		});
	}

}
