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

package com.metreeca.rdf4j.assets;

import com.metreeca.json.Order;
import com.metreeca.json.Shape;
import com.metreeca.json.queries.Items;
import com.metreeca.json.shapes.*;
import com.metreeca.rdf4j.SPARQLScribe;
import com.metreeca.rdf4j.assets.GraphEngine.Options;
import com.metreeca.rest.Scribe;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.json.Frame.traverse;
import static com.metreeca.json.Values.bnode;
import static com.metreeca.json.Values.statement;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.rdf4j.SPARQLScribe.*;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Scribe.*;

import static org.eclipse.rdf4j.model.util.Values.triple;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;

final class GraphQueryItems extends GraphQueryBase {

	private final Options options=options();

	private final Graph graph=asset(graph());


	GraphQueryItems(final Options options) {
		super(options);
	}


	Collection<Statement> process(final IRI resource, final Items items) {

		final Shape shape=items.shape();
		final List<Order> orders=items.orders();
		final int offset=items.offset();
		final int limit=items.limit();

		final int reserved=1+orders.size();

		final Shape filter=shape
				.filter(resource)
				.resolve(resource)
				.label(reserved);

		final Shape convey=shape
				.convey()
				.resolve(resource)
				.label(reserved);

		final Collection<Triple> template=convey.map(new TemplateProbe(root)).collect(toList());
		final Collection<Statement> model=new LinkedHashSet<>();

		// construct results are serialized with no ordering guarantee >> transfer data as tuples to preserve order

		evaluate(() -> graph.exec(connection -> {
			connection.prepareTupleQuery(compile(() -> code(list(

					comment("items query"),

					prefix(OWL.NS),
					prefix(RDFS.NS),

					space(select(list(Stream.concat(

							Stream.of(root), /// always project root

							Stream.concat( // project template variables

									template.stream().map(Triple::getSubject),
									template.stream().map(Triple::getObject)

							).map(BNode.class::cast).map(BNode::getID)

					).distinct().sorted().map(SPARQLScribe::var)))),

					space(where(
							matcher(filter, orders, offset, limit),
							pattern(convey),
							sorters(orders) // !!! (€) don't extract if already present in pattern
					)),

					space(
							order(criteria(orders))
					)

			)))).evaluate(new AbstractTupleQueryResultHandler() {

				@Override public void handleSolution(final BindingSet bindings) {

					final Value match=bindings.getValue(root);

					if ( match != null ) {

						if ( !match.equals(resource) ) {
							model.add(statement(resource, Shape.Contains, match));
						}

						template.forEach(statement -> {

							final Resource subject=statement.getSubject();
							final Value object=statement.getObject();


							final Value source=subject instanceof BNode
									? bindings.getValue(((BNode)subject).getID())
									: subject;

							final Value target=object instanceof BNode
									? bindings.getValue(((BNode)object).getID())
									: object;

							if ( source instanceof Resource && target != null ) {
								model.add(statement((Resource)source, statement.getPredicate(), target));
							}

						});

					}

				}

			});
		}));

		return model;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Scribe matcher(final Shape shape, final List<Order> orders, final int offset, final int limit) {
		return shape.equals(and()) ? nothing() : shape.equals(or()) ? filter(text(false)) : space(block(

				select(true, var(root)),

				block(
						filters(shape),
						with(offset > 0 || limit > 0, sorters(orders))
				),

				with(offset > 0 || limit > 0, order(criteria(orders))),
				offset(offset),
				limit(limit, options.items())

		));
	}

	private Scribe sorters(final List<Order> orders
	) {
		return space(list(orders.stream()
				.filter(order -> !order.path().isEmpty()) // root already retrieved
				.map(order -> optional(edge(
						var(root),
						options.same() ? same(order.path()) : path(order.path()),
						var(valueOf(1+orders.indexOf(order)))
				)))
		));
	}

	private static Scribe criteria(final List<Order> orders) {
		return list(Stream.concat(

				orders.stream().map(order -> sort(order.inverse(),
						order.path().isEmpty() ? var(root) : var(valueOf(1+orders.indexOf(order)))
				)),

				orders.stream()
						.map(Order::path)
						.filter(List::isEmpty)
						.findFirst()
						.map(empty -> Stream.<Scribe>empty())
						.orElseGet(() -> Stream.of(var(root)))  // root as last resort, unless already used

		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TemplateProbe extends Shape.Probe<Stream<Triple>> {

		private final String anchor;


		private TemplateProbe(final String anchor) {
			this.anchor=anchor;
		}


		@Override public Stream<Triple> probe(final Field field) {

			final Shape shape=field.shape();
			final String alias=field.alias();

			final BNode source=bnode(anchor);
			final BNode target=bnode(alias);

			final Triple triple=traverse(field.iri(),
					iri -> triple(source, iri, target),
					iri -> triple(target, iri, source)
			);

			return Stream.concat(Stream.of(triple), shape.map(new TemplateProbe(alias)));
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
