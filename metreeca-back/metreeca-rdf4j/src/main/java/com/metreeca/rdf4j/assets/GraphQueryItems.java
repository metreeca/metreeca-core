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
import com.metreeca.rdf4j.assets.GraphEngine.Options;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.metreeca.json.Frame.traverse;
import static com.metreeca.json.Values.bnode;
import static com.metreeca.json.Values.statement;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Or.or;
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

		final Collection<Triple> template=convey.map(new TemplateProbe(Root)).collect(toList());
		final Collection<Statement> model=new LinkedHashSet<>();

		// construct results are serialized with no ordering guarantee >> transfer data as tuples to preserve order

		evaluate(() -> graph.exec(connection -> {
			connection.prepareTupleQuery(compile(() -> code(text(

					"# items query\n"
							+"\n"
							+"prefix owl: <http://www.w3.org/2002/07/owl#>\n"
							+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
							+"\n"
							+"select {variables} where {\n"
							+"\n"
							+"\t{matcher}\n"
							+"\n"
							+"\t{pattern}\n"
							+"\n"
							+"\t{sorters}\n"
							+"\n"
							+"} order by {criteria}",

					list(Stream.concat(

							Stream.of(Root), /// always project root

							Stream.concat( // project template variables

									template.stream().map(Triple::getSubject),
									template.stream().map(Triple::getObject)

							).distinct().map(BNode.class::cast).map(BNode::getID)

					).distinct().sorted().map(GraphQueryBase::var)),

					matcher(filter, orders, offset, limit),
					pattern(convey),

					sorters(orders), // !!! (€) don't extract if already present in pattern
					criteria(orders)

			)))).evaluate(new AbstractTupleQueryResultHandler() {

				@Override public void handleSolution(final BindingSet bindings) {

					final Value match=bindings.getValue(Root);

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

	private UnaryOperator<Appendable> matcher(
			final Shape shape, final List<Order> orders, final int offset, final int limit) {
		return shape.equals(and()) ? nothing() : shape.equals(or()) ? text("filter (false)") : text(

				"{ select distinct {root} {\n"
						+"\n"
						+"\t{roots}\n"
						+"\n"
						+"\t{filters}\n"
						+"\n"
						+"\t{sorters}\n"
						+"\n"
						+"} {orders} {offset} {limit} }",

				var(Root),

				roots(shape),
				filters(shape),

				offset > 0 || limit > 0 ? sorters(orders) : nothing(),
				offset > 0 || limit > 0 ? text(" order by {criteria}", criteria(orders)) : nothing(),

				offset(offset),
				limit(limit, options.items())

		);
	}


	private UnaryOperator<Appendable> sorters(final List<Order> orders) {
		return list(orders.stream()
				.filter(order -> !order.path().isEmpty()) // root already retrieved
				.map(order -> text("optional { {root} {path} {order} }\n",
						var(Root), path(order.path()), var(valueOf(1+orders.indexOf(order)))
				))
		);
	}

	private static UnaryOperator<Appendable> criteria(final List<Order> orders) {
		return list(Stream.concat(

				orders.stream().map(order -> text(
						order.inverse() ? "desc({criterion})" : "asc({criterion})",
						order.path().isEmpty() ? var(Root) : var(valueOf(1+orders.indexOf(order)))
				)),

				orders.stream()
						.map(Order::path)
						.filter(List::isEmpty)
						.findFirst()
						.map(empty -> Stream.<UnaryOperator<Appendable>>empty())
						.orElseGet(() -> Stream.of(var(Root)))  // root as last resort, unless already used

		), " ");
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
