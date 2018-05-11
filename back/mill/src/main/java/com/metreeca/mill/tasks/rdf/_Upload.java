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

package com.metreeca.mill.tasks.rdf;

import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.metreeca.tray.sys.Trace.clip;


public final class _Upload implements Task { // !!! merge into Graph

	private static final int ConnectionGrace=250; // grace period between repeated backend connections [ms]


	private final IRI context;

	private boolean clear;
	private int batch;


	public _Upload clear(final boolean clear) {

		this.clear=clear;

		return this;
	}

	public _Upload batch(final int batch) {

		if ( batch < 0 ) {
			throw new IllegalArgumentException("illegal batch size ["+batch+"]");
		}

		this.batch=batch;

		return this;
	}

	public _Upload(final IRI context) {

		if ( context == null ) {
			throw new NullPointerException("null context");
		}

		this.context=context;
	}


	@Override public Stream<_Cell> execute(final Tool.Loader tools, final Stream<_Cell> items) {

		final Graph graph=tools.get(Graph.Tool);
		final Trace trace=tools.get(Trace.Tool);

		if ( clear ) {
			graph.update(connection -> {

				connection.clear(context);

				return null;

			});
		}

		final List<Statement> chunk=new ArrayList<>();

		items.forEach(item -> {

			chunk.addAll(item.model());

			if ( batch > 0 && chunk.size() >= batch ) {

				final List<Statement> head=chunk.subList(0, batch);

				try {

					TimeUnit.MILLISECONDS.sleep(ConnectionGrace); // grace period before attempting a new connection

					final long start=System.currentTimeMillis();

					graph.update(connection -> {

						connection.add(head, context);

						return null;

					});

					final long stop=System.currentTimeMillis();

					trace.info(this, String.format("uploaded <%d> statements to <%s> in %d ms",
							head.size(), clip(context), Math.max(stop-start, 1)));

				} catch ( final InterruptedException ignored ) {

				} finally { // !!! optimize

					final Collection<Statement> tail=new ArrayList<>(chunk.subList(batch, chunk.size()));

					chunk.clear();
					chunk.addAll(tail);
				}

			}

		});

		if ( !chunk.isEmpty() ) {

			final long start=System.currentTimeMillis();

			graph.update(connection -> {

				connection.add(chunk, context);

				return null;

			});

			final long stop=System.currentTimeMillis();

			trace.info(this, String.format("uploaded <%d> statements to <%s> in %d ms",
					chunk.size(), clip(context), Math.max(stop-start, 1)));
		}

		return Stream.of(_Cell.cell(context));
	}
}
