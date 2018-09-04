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
import com.metreeca.form.things.Values;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static com.metreeca.form.things.Values.bnode;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.tray.Tray.tool;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;


public final class _Query implements Task { // !!! merge into SPARQL?

	private final IRI context;

	private final String query;
	private final String base;

	private final Graph graph=tool(Graph.Factory);
	private final Trace trace=tool(Trace.Factory);


	public _Query(final IRI context, final String query) {
		this(context, query, "");
	}

	public _Query(final IRI context, final String query, final String base) {

		if ( context == null ) {
			throw new NullPointerException("null context");
		}

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		this.context=context;

		this.query=query;
		this.base=base;
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) {

		try (final RepositoryConnection connection=graph.connect()) {

		}

		return graph.update(connection -> {

			final SimpleDataset dataset=new SimpleDataset(); // !!! factor with _Update

			dataset.setDefaultInsertGraph(context);
			dataset.addDefaultRemoveGraph(context);

			dataset.addDefaultGraph(context);
			dataset.addNamedGraph(context);

			items.map(_Cell::focus).forEach(graph -> {
				if ( graph instanceof IRI ) {
					dataset.addDefaultGraph((IRI)graph);
					dataset.addNamedGraph((IRI)graph);
				}
			});

			final Query query=connection.prepareQuery(QueryLanguage.SPARQL, this.query, base);

			query.setDataset(dataset);

			final List<_Cell> cells=new ArrayList<>();

			final long start=currentTimeMillis();

			if ( query instanceof TupleQuery ) {

				((TupleQuery)query).evaluate(new AbstractTupleQueryResultHandler() {
					@Override public void handleSolution(final BindingSet bindings) {

						final Value thiz=bindings.getValue("this");
						final Resource focus=thiz instanceof Resource ? (Resource)thiz : bnode();

						final Collection<Statement> model=new ArrayList<>();

						for (final Binding binding : bindings) {

							final String name=binding.getName();
							final Value value=binding.getValue();

							if ( !name.equals("this") ) {
								model.add(statement(focus, iri(Values.Internal, name), value));
							}
						}

						cells.add(_Cell.cell(focus, model));
					}
				});

			} else {

				throw new UnsupportedOperationException("unsupported query type ["+query.getClass().getName()+"]");

			}

			final long stop=currentTimeMillis();

			final long elapsed=stop-start;

			trace.info(this, format("generated %d cells in %d ms", cells.size(), max(elapsed, 1)));

			return cells.stream();

		});
	}

}
