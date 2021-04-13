/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rdf4j.services;

import com.metreeca.json.*;
import com.metreeca.json.queries.*;
import com.metreeca.rest.Config;
import com.metreeca.rest.Setup;
import com.metreeca.rest.services.Engine;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Toolbox.service;


/**
 * Model-driven graph engine.
 *
 * <p>Handles model-driven CRUD operations on linked data resources stored in the shared {@linkplain Graph graph}.</p>
 */
public final class GraphEngine extends Setup<GraphEngine> implements Engine {

	private final Graph graph=service(graph());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Frame> create(final Frame frame, final Shape shape) {
		return graph.update(connection -> Optional.of(frame.focus())

				.filter(item ->
						!connection.hasStatement(item, null, null, true)
				)

				.map(item -> {

					connection.add(frame.model());

					return frame;

				})
		);
	}

	@Override public Optional<Frame> relate(final Frame frame, final Query query) {
		return Optional

				.of(query.map(new QueryProbe(this, frame.focus())))

				.map(model -> frame(frame.focus(), model));
	}

	@Override public Optional<Frame> update(final Frame frame, final Shape shape) {
		return graph.update(connection -> Optional

				.of(items(shape).map(new QueryProbe(this, frame.focus())))

				.filter(model -> !model.isEmpty())

				.map(model -> {

					connection.remove(model);
					connection.add(frame.model());

					return frame;

				})
		);
	}

	@Override public Optional<Frame> delete(final Frame frame, final Shape shape) {
		return graph.update(connection -> Optional

				.of(items(shape).map(new QueryProbe(this, frame.focus())))

				.filter(model -> !model.isEmpty())

				.map(model -> {

					connection.remove(model);

					return frame(frame.focus(), model);

				})
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class QueryProbe extends Query.Probe<Collection<Statement>> {

		private final Config config;
		private final Resource resource;


		QueryProbe(final Config config, final Resource resource) {
			this.config=config;
			this.resource=resource;
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		@Override public Collection<Statement> probe(final Items items) {
			return new GraphItems(config).process(resource, items);
		}

		@Override public Collection<Statement> probe(final Terms terms) {
			return new GraphTerms(config).process(resource, terms);
		}

		@Override public Collection<Statement> probe(final Stats stats) {
			return new GraphStats(config).process(resource, stats);
		}

	}

}
