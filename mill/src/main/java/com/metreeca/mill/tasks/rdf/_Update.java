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
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;

import java.util.stream.Stream;

import static com.metreeca.tray.Tray.tool;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;


public final class _Update implements Task { // !!! merge into SPARQL?

	private final Graph graph=tool(Graph.Tool);
	private final Trace trace=tool(Trace.Tool);

	private final IRI context;

	private final String update;
	private final String base;


	public _Update(final IRI context, final String update) {
		this(context, update, "");
	}

	public _Update(final IRI context, final String update, final String base) {

		if ( context == null ) {
			throw new NullPointerException("null context");
		}

		if ( update == null ) {
			throw new NullPointerException("null update");
		}

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		this.context=context;

		this.update=update;
		this.base=base;
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) {
		return graph.update(connection -> {

			final SimpleDataset dataset=new SimpleDataset(); // !!! factor with _Query

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

			final Update update=connection.prepareUpdate(QueryLanguage.SPARQL, this.update, base);

			final long start=currentTimeMillis();
			final long before=connection.size(context);

			update.setDataset(dataset);
			update.execute();

			final long stop=currentTimeMillis();
			final long after=connection.size(context);

			final long elapsed=stop-start;
			final long delta=after-before;

			trace.info(this, format("%s %d statements in %d ms",
					delta >= 0 ? "inserted" : "deleted", delta, max(elapsed, 1)));

			return Stream.of(_Cell.cell(context));

		});
	}

}
