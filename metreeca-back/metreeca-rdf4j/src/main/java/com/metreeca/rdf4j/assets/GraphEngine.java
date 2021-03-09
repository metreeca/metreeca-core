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

import com.metreeca.json.Shape;
import com.metreeca.rest.*;
import com.metreeca.rest.assets.Engine;
import com.metreeca.rest.formats.JSONLDFormat;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;

import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rdf4j.assets.Graph.txn;
import static com.metreeca.rest.Context.asset;


/**
 * Model-driven graph engine.
 *
 * <p>Manages graph transactions and handles model-driven CRUD actions on linked data resources stored in the shared
 * {@linkplain Graph graph}.</p>
 */
public final class GraphEngine implements Engine {

	private boolean same;

	private int items=1_000; // the maximum number of resources to be returned from items queries
	private int stats=10_000; // the maximum number of resources to be evaluated by stats queries
	private int terms=10_000; // the maximum number of resources to be evaluated by terms queries


	private final Graph graph=asset(graph());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures {@link OWL#SAMEAS owl:sameAs} query rewriting.
	 *
	 * @param same if {@code true}, enable query-rewriting to support {@code owl:sameAs} reasoning
	 *
	 * @return this graph engine
	 */
	public GraphEngine same(final boolean same) {

		this.same=same;

		return this;
	}


	GraphEngine items(final int items) {

		if ( items < 0 ) {
			throw new IllegalArgumentException("negative items sampling limit");
		}

		this.items=items;

		return this;
	}

	GraphEngine stats(final int stats) {

		if ( stats < 0 ) {
			throw new IllegalArgumentException("negative stats sampling limit");
		}

		this.stats=stats;

		return this;
	}

	GraphEngine terms(final int terms) {

		if ( terms < 0 ) {
			throw new IllegalArgumentException("negative terms sampling limit");
		}

		this.terms=terms;

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

		return new GraphActorCreator().handle(request);
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

		return new GraphActorRelator(new Options(this)).handle(request);
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

		return new GraphActorBrowser(new Options(this)).handle(request);
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

		return new GraphActorUpdater(new Options(this)).handle(request);
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

		return new GraphActorDeleter(new Options(this)).handle(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static final class Options {

		private final GraphEngine engine;

		Options(final GraphEngine engine) {
			this.engine=engine;
		}


		public boolean same() { return engine.same; }


		public int items() { return engine.items; }

		public int terms() { return engine.terms; }

		public int stats() { return engine.stats; }

	}

}
