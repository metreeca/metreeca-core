/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.feed.rdf;

import com.metreeca.rdf.Values;
import com.metreeca.rest.services.Logger;
import com.metreeca.rdf4j.services.Graph;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Logger.logger;

import static java.util.stream.Collectors.joining;


public final class Upload implements Consumer<Collection<Statement>> {

	private static final Resource[] DefaultContexts=new Resource[0];


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Graph graph=service(Graph.graph());
	private Resource[] contexts=DefaultContexts;

	private final AtomicBoolean clear=new AtomicBoolean();

	private final Logger logger=service(logger());


	public Upload graph(final Graph graph) {

		if ( graph == null ) {
			throw new NullPointerException("null graph");
		}

		this.graph=graph;

		return this;
	}

	public Upload contexts(final Resource... contexts) {

		if ( contexts == null || Arrays.stream(contexts).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null contexts");
		}

		this.contexts=contexts.clone();

		return this;
	}

	public Upload clear(final boolean clear) {

		this.clear.set(clear);

		return this;
	}


	@Override public void accept(final Collection<Statement> model) {

		final String contexts=this.contexts.length == 0 ? "default context" : Arrays.stream(this.contexts)
				.map(Values::format)
				.collect(joining(", "));

		final long start=System.currentTimeMillis();

		graph.exec(connection -> {

			if ( clear.getAndSet(false) ) {

				connection.clear(this.contexts);

				logger.info(this, String.format(
						"cleared %s", contexts
				));
			}

			if ( !model.isEmpty() ) {
				connection.add(model, this.contexts);
			}

		});

		final long stop=System.currentTimeMillis();

		logger.info(this, String.format(
				"uploaded <%d> statements to %s in %d ms", model.size(), contexts, Math.max(stop-start, 1)
		));

	}

}
