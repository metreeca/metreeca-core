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
import com.metreeca.json.queries.Items;
import com.metreeca.json.shapes.*;
import com.metreeca.rest.Config;
import com.metreeca.rest.services.Engine;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.rdf4j.SPARQLScribe.*;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Scribe.*;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.Xtream.task;

import static org.eclipse.rdf4j.model.util.Values.triple;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

final class GraphItems extends GraphFacts {

	private final Config config=config();

	private final Graph graph=service(graph());


	GraphItems(final Config config) {
		super(config);
	}


	Frame process(final Value focus, final Items items) {

		final Shape shape=items.shape();
		final List<Order> orders=items.orders();
		final int offset=items.offset();
		final int limit=items.limit();

		final Shape filter=shape
				.filter(focus)
				.resolve(focus)
				.label(this::label);

		final Shape convey=shape
				.convey()
				.resolve(focus)
				.label(this::label);

		final Shape follow=and(orders.stream().map(Order::path).map(path -> path(convey, path)));
		final Collection<Triple> template=convey.map(new TemplateProbe(root)).collect(toList());

		final Map<Value, Collection<Statement>> models=new LinkedHashMap<>();

		evaluate(() -> graph.query(task(connection -> {
			connection.prepareTupleQuery(compile(() -> code(list(

					comment("items query"),

					prefix(OWL.NS),
					prefix(RDFS.NS),

					space(select(), where(

							space(block(

									select(true, var(root)), // transfer matches as tuples to preserve order

									block(

											space(tree(filter, true)),
											space(tree(follow, false))

									),

									order(list(Stream.concat(

											orders.stream().map(order ->
													sort(order.inverse(), var(hook(follow, order.path())))
											),

											Stream.of(asc(var(root))).filter(s -> // then root, unless already included
													orders.stream().map(Order::path).noneMatch(List::isEmpty)
											)

									))),

									offset(offset),
									limit(limit, config.get(Engine::ItemsLimit))

							)),

							tree(convey, false)

					))

			)))).evaluate(new AbstractTupleQueryResultHandler() {

				@Override public void handleSolution(final BindingSet bindings) {

					final Value match=bindings.getValue(root);

					if ( match != null ) {

						models.compute(match, (key, value) -> {

							final Collection<Statement> model=value != null ? value : new LinkedHashSet<>();

							template.forEach(statement -> {

								final Resource subject=statement.getSubject();
								final Value object=statement.getObject();

								final Value source=subject instanceof BNode
										? bindings.getValue(principal(((BNode)subject).getID(),
										bindings.getBindingNames()))
										: subject;

								final Value target=object instanceof BNode
										? bindings.getValue(principal(((BNode)object).getID(),
										bindings.getBindingNames()))
										: object;

								if ( source instanceof Resource && target != null ) {
									model.add(statement((Resource)source, statement.getPredicate(), target));
								}

							});

							return model;

						});

					}

				}

			});
		})));

		return frame(focus, models.getOrDefault(focus, emptySet()))

				.frames(Shape.Contains, models.entrySet().stream()
						.filter(entry -> !entry.getKey().equals(focus))
						.map(entry -> frame(entry.getKey(), entry.getValue()))
				);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TemplateProbe extends Shape.Probe<Stream<Triple>> {

		private final String anchor;


		private TemplateProbe(final String anchor) {
			this.anchor=anchor;
		}


		@Override public Stream<Triple> probe(final Link link) {
			return link.shape().map(this);
		}

		@Override public Stream<Triple> probe(final Field field) {

			final String label=field.label();
			final Shape shape=field.shape();

			final BNode source=bnode(anchor);
			final BNode target=bnode(label);

			final Triple triple=traverse(field.iri(),
					iri -> triple(source, iri, target),
					iri -> triple(target, iri, source)
			);

			return Stream.concat(Stream.of(triple), shape.map(new TemplateProbe(label)));
		}


		@Override public Stream<Triple> probe(final When when) {
			return Stream.concat(
					when.pass().map(this),
					when.fail().map(this)
			);
		}

		@Override public Stream<Triple> probe(final And and) {
			return and.shapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Triple> probe(final Or or) {
			return or.shapes().stream().flatMap(shape -> shape.map(this));
		}


		@Override public Stream<Triple> probe(final Shape shape) { return Stream.empty(); }

	}

}
