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

import com.metreeca.json.*;
import com.metreeca.json.queries.Items;
import com.metreeca.json.shapes.*;
import com.metreeca.rdf4j.SPARQLScribe;
import com.metreeca.rdf4j.assets.GraphEngine.Options;

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
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Field.fields;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static com.metreeca.rdf4j.SPARQLScribe.*;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Scribe.block;
import static com.metreeca.rest.Scribe.code;
import static com.metreeca.rest.Scribe.indent;
import static com.metreeca.rest.Scribe.list;
import static com.metreeca.rest.Scribe.space;
import static com.metreeca.rest.Scribe.when;

import static org.eclipse.rdf4j.model.util.Values.triple;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
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

		final Shape filter=shape
				.filter(resource)
				.resolve(resource)
				.label(this::label);

		final Shape convey=shape
				.convey()
				.resolve(resource)
				.label(this::label);

		final Collection<Triple> template=convey.map(new TemplateProbe(root)).collect(toList());
		final Map<List<IRI>, String> sorting=sorting(filter, convey, orders);

		final Collection<Statement> model=new LinkedHashSet<>();

		// construct results are serialized with no ordering guarantee >> transfer data as tuples to preserve order

		evaluate(() -> graph.exec(connection -> {
			connection.prepareTupleQuery(compile(() -> code(list(

					comment("items query"),

					prefix(OWL.NS),
					prefix(RDFS.NS),

					space(select(space(indent(

							list(projection(template).map(SPARQLScribe::var))

					)))),

					space(where(

							space(block(

									select(true, var(root)),

									block(

											space(filters(filter)),

											// non-filtering sorting variables; use pattern to honor convey constraints

											space(pattern(convey.map(new SortingProbe(sorting))))

									),

									order(

											list(orders.stream().map(order ->
													sort(order.inverse(), var(sorting.get(order.path())))
											)),

											// sort on root as last resort, unless already included

											when(!sorting.containsValue(root), asc(var(root)))

									),

									offset(offset),
									limit(limit, options.items())

							)),

							pattern(convey)

					))

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

	private Stream<String> projection(final Collection<Triple> template) {
		return Stream.concat(

				Stream.of(root), /// always project root

				Stream.concat( // project template variables

						template.stream().map(Triple::getSubject),
						template.stream().map(Triple::getObject)

				).map(BNode.class::cast).map(BNode::getID)

		).distinct().sorted();
	}

	private Map<List<IRI>, String> sorting(final Shape filter, final Shape convey, final Collection<Order> orders) {

		final Map<List<IRI>, String> labels=new HashMap<>();

		orders.stream().map(Order::path).distinct().forEach(path -> {

			Optional.of(root).filter(label -> path.isEmpty()).ifPresent(label -> labels.putIfAbsent(path, label));

			field(filter, path).map(Field::alias).ifPresent(label -> labels.putIfAbsent(path, label));
			field(convey, path).map(Field::alias).ifPresent(label -> labels.putIfAbsent(path, label));

			if ( !labels.containsKey(path) ) {
				throw new IllegalArgumentException(format("unknown sorting path %s",
						path.stream().map(Values::format).collect(joining("/"))
				));
			}

		});

		return labels;
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

	private static final class SortingProbe extends Shape.Probe<Shape> {

		private final Map<List<IRI>, String> labels;


		SortingProbe(final Map<List<IRI>, String> labels) {this.labels=labels;}


		@Override public Shape probe(final Field field) {

			final String alias=field.alias();
			final IRI iri=field.iri();
			final Shape shape=field.shape().map(this);

			return labels.containsValue(alias) || fields(shape).findFirst().isPresent() ?
					field(alias, iri, shape) : and();
		}


		@Override public Shape probe(final When when) {
			return when(when.test().map(this), when.pass().map(this), when.fail().map(this));
		}

		@Override public Shape probe(final And and) {
			return and(and.shapes().stream().map(this));
		}

		@Override public Shape probe(final Or or) {
			return or(or.shapes().stream().map(this));
		}


		@Override public Shape probe(final Shape shape) {
			return shape;
		}

	}

}
