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

import com.metreeca.json.Query;
import com.metreeca.json.Shape;
import com.metreeca.json.queries.*;
import com.metreeca.rdf4j.services.GraphFacts.Options;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.JSONLDFormat;
import com.metreeca.rest.services.Engine;

import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;

import static com.metreeca.json.Values.IRIPattern;
import static com.metreeca.json.Values.format;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.Convey;
import static com.metreeca.json.shapes.Guard.Mode;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rdf4j.services.Graph.txn;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.formats.JSONLDFormat.*;
import static com.metreeca.rest.services.Engine.StatsShape;
import static com.metreeca.rest.services.Engine.TermsShape;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;


/**
 * Model-driven graph engine.
 *
 * <p>Manages graph transactions and handles model-driven CRUD actions on linked data resources stored in the shared
 * {@linkplain Graph graph}.</p>
 */
public final class GraphEngine implements Engine {

	/**
	 * Maximum number of resources returned by items queries.
	 *
	 * @return an {@linkplain #set(Supplier, Object) option} with a default value of {@code 1000}
	 */
	public static Supplier<Integer> items() {
		return () -> 1_000;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<Supplier<?>, Object> options=new LinkedHashMap<>();

	private final Graph graph=service(graph());


	/**
	 * Retrieves an engine option.
	 *
	 * @param option the option to be retrieved; must return a non-null default value
	 * @param <V>    the type of the option to be retrieved
	 *
	 * @return the value previously {@linkplain #set(Supplier, Object) configured} for {@code option} or its default
	 * value, if no custom value was configured; in the latter case the returned value is cached
	 *
	 * @throws NullPointerException if {@code option} is null or returns a null value
	 */
	@SuppressWarnings("unchecked")
	private <V> V get(final Supplier<V> option) {

		if ( option == null ) {
			throw new NullPointerException("null option");
		}

		return (V)options.computeIfAbsent(option, key ->
				requireNonNull(key.get(), "null option return value")
		);
	}

	/**
	 * Configures an engine option.
	 *
	 * @param option the option to be configured; must return a non-null default value
	 * @param value  the value to be configured for {@code option}
	 * @param <V>    the type of the option to be configured
	 *
	 * @return this engine
	 *
	 * @throws NullPointerException if either {@code option} or {@code value} is null
	 */
	public <V> GraphEngine set(final Supplier<V> option, final V value) {

		if ( option == null ) {
			throw new NullPointerException("null option");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		options.put(option, value);

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null task");
		}

		return request -> consumer -> graph.exec(txn(connection -> {
			handler.handle(request).accept(consumer);
		}));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a linked data resource.
	 *
	 * <p>Handles creation requests on the linked data container identified by the request {@linkplain Request#item()
	 * focus item}:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is expected to include a resource {@linkplain JSONLDFormat#shape() shape};</li>
	 *
	 * <li>the request {@link JSONLDFormat JSON-LD} body is expected to contain a description of the resource to be
	 * created matching the shape using the request {@linkplain Request#item() item} as subject;</li>
	 *
	 * <li>the resource to be created is assigned a unique IRI based on the stem of the the request IRI and the value
	 * of the {@code Slug} request header, if one is found, or a random id, otherwise;</li>
	 *
	 * <li>the request body is rewritten to the assigned IRI and stored into the shared {@linkplain Graph graph};</li>
	 *
	 * <li>the target container identified by the request item is connected to the newly created resource as
	 * {@linkplain Shape#outline(Value...) outlined} in the filtering constraints in the request shape;</li>
	 *
	 * <li>the operation is completed with a {@value Response#Created} status code;</li>
	 *
	 * <li>the IRI of the newly created resource is advertised through the {@code Location} HTTP response header.</li>
	 *
	 * </ul>
	 *
	 * @param request the request to be handled
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to {@code request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	@Override public Future<Response> create(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		final IRI item=iri(request.item());
		final Shape shape=request.attribute(shape());

		return request.body(jsonld()).fold(request::reply, model ->
				request.reply(response -> graph.exec(txn(connection -> {

					final boolean clashing=connection.hasStatement(item, null, null, true)
							|| connection.hasStatement(null, null, item, true);

					if ( clashing ) { // report clash

						return response.map(status(InternalServerError,
								new IllegalStateException(format("clashing resource identifier %s", format(item)))
						));

					} else { // store model

						connection.add(shape.outline(item));
						connection.add(model);

						final String location=item.stringValue();

						return response.status(Created)
								.header("Location", Optional // root-relative to support relocation
										.of(item.stringValue())
										.map(IRIPattern::matcher)
										.filter(Matcher::matches)
										.map(matcher -> matcher.group("pathall"))
										.orElse(location)
								);

					}

				})))
		);
	}

	/**
	 * Retrieves a linked data resource.
	 *
	 * <p>Handles retrieval requests on the linked data resource identified by the request {@linkplain Request#item()
	 * focus item}.</p>
	 *
	 * <p>If the shared {@linkplain  Graph graph} actually contains a resource matching the request focus item IRI:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is expected to include a resource {@linkplain JSONLDFormat#shape() shape};</li>
	 *
	 * <li>the response includes the derived shape actually used in the retrieval process;</li>
	 *
	 * <li>the response {@link JSONLDFormat JSON-LD} body contains a description of the request item retrieved from the
	 * shared {@linkplain  Graph graph} and matching the response shape;</li>
	 *
	 * <li>the operation is completed with a {@value Response#OK} status code.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise:</p>
	 *
	 * <ul>
	 *
	 * <li>the operation is reported as unsuccessful with a {@value Response#NotFound} status code.</li>
	 *
	 * </ul>
	 *
	 * @param request the request to be handled
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to {@code request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	@Override public Future<Response> relate(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		final IRI item=iri(request.item());
		final Shape shape=and(all(item), request.attribute(shape()));

		return query(item, shape, request.query()).fold(request::reply, query ->
				request.reply(response -> Optional

						.of(query.map(new QueryProbe(item, this::get)))

						.filter(model -> !model.isEmpty())

						.map(model -> response.status(OK)
								.attribute(shape(), query.map(new ShapeProbe(false)))
								.body(jsonld(), model)
						)

						.orElseGet(() -> response.status(NotFound)) // !!! 410 Gone if previously known
				)
		);
	}

	/**
	 * Browses a linked data container.
	 *
	 * <p>Handles browsing requests on the linked data container identified by the request {@linkplain Request#item()
	 * focus item}:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is expected to include a resource {@linkplain JSONLDFormat#shape() shape};</li>
	 *
	 * <li>the response includes the derived shape actually used in the retrieval process;</li>
	 *
	 * <li>the response {@link JSONLDFormat JSON-LD} body contains a description of member linked data resources
	 * retrieved from the shared {@linkplain  Graph graph} according to the filtering constraints in the request shape
	 * and matching the response shape; the IRI of the target container is connected to the IRIs of the member
	 * resources using the {@link Shape#Contains ldp:contains} property;</li>
	 *
	 * <li>the operation is completed with a {@value Response#OK} status code.</li>
	 *
	 * </ul>
	 *
	 * @param request the request to be handled
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to {@code request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	@Override public Future<Response> browse(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		final IRI item=iri(request.item());
		final Shape shape=request.attribute(shape());

		return query(item, shape, request.query()).fold(request::reply, query ->
				request.reply(response -> response.status(OK) // containers are virtual and respond always with 200 OK
						.attribute(shape(), query.map(new ShapeProbe(true)))
						.body(jsonld(), query.map(new QueryProbe(item, this::get)))
				)
		);
	}

	/**
	 * Updates a linked data resource.
	 *
	 * <p>Handles updating requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item}.</p>
	 *
	 * <p>If the shared {@linkplain  Graph graph} actually contains a resource matching the request focus item IRI:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is expected to include a resource {@linkplain JSONLDFormat#shape() shape};</li>
	 *
	 * <li>the request {@link JSONLDFormat JSON-LD} body is expected to contain a description of the resource to be
	 * updated matching by the shape;</li>
	 *
	 * <li>the existing description of the resource matching the request shape is replaced in the shared
	 * {@linkplain Graph graph} with the request body;</li>
	 *
	 * <li>the operation is completed with a {@value Response#NoContent} status code.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise:</p>
	 *
	 * <ul>
	 *
	 * <li>the operation is reported with a {@value Response#NotFound} status code.</li>
	 *
	 * </ul>
	 *
	 * @param request the request to be handled
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to {@code request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	@Override public Future<Response> update(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		final IRI item=iri(request.item());
		final Shape shape=request.attribute(shape());

		return request.body(jsonld()).fold(request::reply, model ->
				request.reply(response -> graph.exec(txn(connection -> {

					return Optional

							.of(Items.items(shape).map(new QueryProbe(item, this::get)))

							.filter(current -> !current.isEmpty())

							.map(current -> {

								connection.remove(current);
								connection.add(model);

								return response.status(NoContent);

							})

							.orElseGet(() -> response.status(NotFound)); // !!! 410 Gone if previously known

				})))
		);
	}

	/**
	 * Deletes a linked data resource.
	 *
	 * <p>Handles deletion requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item}.</p>
	 *
	 * <p>If the shared {@linkplain  Graph graph} actually contains a resource matching the request focus item IRI:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is expected to include a resource {@linkplain JSONLDFormat#shape() shape};</li>
	 *
	 * <li>the existing description of the resource matching the request shape is removed from the shared
	 * {@linkplain Graph graph};</li>
	 *
	 * <li>the operation is completed with a {@value Response#NoContent} status code.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise:</p>
	 *
	 * <ul>
	 *
	 * <li>the operation is reported with a {@value Response#NotFound} status code.</li>
	 *
	 * </ul>
	 *
	 * @param request the request to be handled
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to {@code request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	@Override public Future<Response> delete(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		final IRI item=iri(request.item());
		final Shape shape=request.attribute(shape());

		return request.reply(response -> graph.exec(txn(connection -> {

			return Optional

					.of(Items.items(shape).map(new QueryProbe(item, this::get)))

					.filter(current -> !current.isEmpty())

					.map(current -> {

						connection.remove(shape.outline(item));
						connection.remove(current);

						return response.status(NoContent);

					})

					.orElseGet(() -> response.status(NotFound)); // !!! 410 Gone if previously known

		})));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class QueryProbe extends Query.Probe<Collection<Statement>> {

		private final IRI resource;
		private final Options options;


		QueryProbe(final IRI resource, final Options options) {
			this.resource=resource;
			this.options=options;
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		@Override public Collection<Statement> probe(final Items items) {
			return new GraphItems(options).process(resource, items);
		}

		@Override public Collection<Statement> probe(final Terms terms) {
			return new GraphTerms(options).process(resource, terms);
		}

		@Override public Collection<Statement> probe(final Stats stats) {
			return new GraphStats(options).process(resource, stats);
		}

	}

	private static final class ShapeProbe extends Query.Probe<Shape> {

		private final boolean container;


		private ShapeProbe(final boolean container) {
			this.container=container;
		}


		@Override public Shape probe(final Items items) { // !!! add Shape.Contains if items.path is not empty
			return (container ?

					field(Shape.Contains, items.shape()) : items.shape()

			).redact(Mode, Convey); // remove filters
		}

		@Override public Shape probe(final Stats stats) {
			return StatsShape(stats);
		}

		@Override public Shape probe(final Terms terms) {
			return TermsShape(terms);
		}

	}

}
