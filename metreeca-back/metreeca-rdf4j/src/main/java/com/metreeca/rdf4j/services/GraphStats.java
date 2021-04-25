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

import com.metreeca.json.Frame;
import com.metreeca.json.Shape;
import com.metreeca.json.queries.Stats;
import com.metreeca.rest.Config;
import com.metreeca.rest.services.Engine;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;

import java.math.BigInteger;
import java.util.*;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.rdf4j.SPARQLScribe.is;
import static com.metreeca.rdf4j.SPARQLScribe.*;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Scribe.indent;
import static com.metreeca.rest.Scribe.text;
import static com.metreeca.rest.Scribe.*;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.Xtream.task;

final class GraphStats extends GraphFacts {

	private final Graph graph=service(graph());


	GraphStats(final Config config) {
		super(config);
	}


	Frame process(final Value focus, final Stats stats) {
		if ( focus.isResource() ) {

			final Shape shape=stats.shape();
			final List<IRI> path=stats.path();
			final int offset=stats.offset();
			final int limit=stats.limit();

			final Shape filter=shape
					.filter(focus)
					.resolve(focus); // .filter() may introduce focus values › resolve afterwards

			final Shape convey=shape
					.convey()
					.resolve(focus);

			final Shape select=and(filter, path(convey, path)).label(this::label); // requires path to exist in convey

			final String hook=hook(select, path);

			final Collection<Statement> model=new LinkedHashSet<>();

			final Map<Value, BigInteger> counts=new HashMap<>();

			final Collection<Value> mins=new ArrayList<>();
			final Collection<Value> maxs=new ArrayList<>();

			evaluate(() -> graph.query(task(connection -> {
				connection.prepareTupleQuery(compile(() -> code(list(

						comment("stats query"),

						prefix(NS),
						prefix(OWL.NS),
						prefix(RDFS.NS),

						space(select(), where(

								space(block(

										space(select(space(indent(

												var("type"),
												as("min", min(var(hook))),
												as("max", max(var(hook))),
												as("count", count(false, var(hook)))

										)))),

										space(where(

												space(tree(select, true)),

												space(bind("type", is(
														isBlank(var(hook)),
														text(":bnode"),
														is(
																isIRI(var(hook)),
																text(":iri"),
																datatype(var(hook))
														)
												)))

												// !!! sampling w/ options.stats()

										)),

										space(
												line(group(var("type"))),
												line(having(gt(count(true, var(hook)), text(0)))),
												line(order(desc(var("count")), var("type"))),
												line(offset(offset)),
												line(limit(limit))
										)

								)),

								space(
										line(optional(edge(var("type"), "rdfs:label", var("type_label")))),
										line(optional(edge(var("type"), "rdfs:comment", var("type_notes"))))
								),

								space(
										line(optional(edge(var("min"), "rdfs:label", var("min_label")))),
										line(optional(edge(var("min"), "rdfs:comment", var("min_notes"))))
								),

								space(
										line(optional(edge(var("max"), "rdfs:label", var("max_label")))),
										line(optional(edge(var("max"), "rdfs:comment", var("max_notes"))))
								)

						))

				)))).evaluate(new AbstractTupleQueryResultHandler() {

					@Override public void handleSolution(final BindingSet bindings) {

						final Resource type=(Resource)bindings.getValue("type");

						final Value type_label=bindings.getValue("type_label");
						final Value type_notes=bindings.getValue("type_notes");

						final Value min=bindings.getValue("min");
						final Value max=bindings.getValue("max");

						final Value min_label=bindings.getValue("min_label");
						final Value min_notes=bindings.getValue("min_notes");

						final Value max_label=bindings.getValue("max_label");
						final Value max_notes=bindings.getValue("max_notes");

						// ;(virtuoso) counts are returned as xsd:int… cast to stay consistent

						final BigInteger count=integer(bindings.getValue("count")).orElse(BigInteger.ZERO);

						model.add(statement((Resource)focus, Engine.stats, type));
						model.add(statement(type, Engine.count, literal(count)));

						if ( type_label != null ) { model.add(statement(type, RDFS.LABEL, type_label)); }
						if ( type_notes != null ) { model.add(statement(type, RDFS.COMMENT, type_notes)); }

						if ( min != null ) { model.add(statement(type, Engine.min, min)); }
						if ( max != null ) { model.add(statement(type, Engine.max, max)); }

						if ( min_label != null ) { model.add(statement((Resource)min, RDFS.LABEL, min_label)); }
						if ( min_notes != null ) { model.add(statement((Resource)min, RDFS.COMMENT, min_notes)); }

						if ( max_label != null ) { model.add(statement((Resource)max, RDFS.LABEL, max_label)); }
						if ( max_notes != null ) { model.add(statement((Resource)max, RDFS.COMMENT, max_notes)); }

						counts.putIfAbsent(type, count);

						if ( min != null ) { mins.add(min); }
						if ( max != null ) { maxs.add(max); }

					}

				});
			})));

			model.add(statement((Resource)focus, Engine.count, literal(counts.values().stream()
					.reduce(BigInteger.ZERO, BigInteger::add)
			)));

			mins.stream()
					.reduce((x, y) -> compare(x, y) < 0 ? x : y)
					.ifPresent(min -> model.add(statement((Resource)focus, Engine.min, min)));

			maxs.stream()
					.reduce((x, y) -> compare(x, y) > 0 ? x : y)
					.ifPresent(max -> model.add(statement((Resource)focus, Engine.max, max)));

			return frame(focus, model);

		} else { return frame(focus); }
	}

}
