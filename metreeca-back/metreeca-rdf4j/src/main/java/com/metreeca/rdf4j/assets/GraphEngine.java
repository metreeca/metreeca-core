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

package com.metreeca.rdf4j.assets;

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Guard;
import com.metreeca.rdf.formats.RDFFormat;
import com.metreeca.rest.*;
import com.metreeca.rest.assets.Engine;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.function.Supplier;

import static com.metreeca.json.Shape.*;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rest.Context.asset;


/**
 * Model-driven graph engine.
 *
 * <p>Manages graph transactions and handles model-driven CRUD actions on LDP resources stored in the shared
 * {@linkplain Graph graph}.</p>
 *
 * @see <a href="https://www.w3.org/TR/ldp/">Linked Data Platform 1.0</a>
 */
public final class GraphEngine implements Engine {

	static final String Base="app:/terms#";

	static final IRI terms=iri(Base, "terms");
	static final IRI stats=iri(Base, "stats");

	static final IRI value=iri(Base, "value");
	static final IRI count=iri(Base, "count");

	static final IRI max=iri(Base, "max");
	static final IRI min=iri(Base, "min");


	private static final Shape TermShape=and(
			field(RDFS.LABEL, optional(), datatype(XSD.STRING))
	);

	static final Shape TermsShape=and(
			field(terms, multiple(),
					field(value, required(), TermShape),
					field(count, required(), datatype(XSD.INTEGER))
			)
	);

	static final Shape StatsShape=and(

			field(count, required(), datatype(XSD.INTEGER)),
			field(min, optional(), TermShape),
			field(max, optional(), TermShape),

			field(stats, multiple(),
					field(count, required(), datatype(XSD.INTEGER)),
					field(min, required(), TermShape),
					field(max, required(), TermShape)
			)

	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=asset(graph());

	private final GraphCreator creator=new GraphCreator();
	private final GraphRelator relator=new GraphRelator();
	private final GraphUpdater updater=new GraphUpdater();
	private final GraphDeleter deleter=new GraphDeleter();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <R> R exec(final Supplier<R> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return graph.exec(connection -> { return task.get(); });
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an LDP resource.
	 *
	 * <p>Handles creation requests on the linked data container identified by the request {@linkplain Request#item()
	 * focus item}, according to the following operating modes.</p>
	 *
	 * <p>If the request target is a {@linkplain Request#collection() collection}:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is expected to include a member resource {@linkplain Shape#shape() shape};</li>
	 *
	 * <li>the request {@link RDFFormat RDF} body is expected to contain an RDF description of the resource to be
	 * created matched by the shape using the request {@linkplain Request#item() item} as subject;</li>
	 *
	 * <li>the resource to be created is assigned a unique IRI based on the stem of the the request IRI and the value
	 * of the {@code Slug} request header, if one is found, or a random UUID, otherwise;</li>
	 *
	 * <li>the request RDF body is rewritten to the assigned IRI and stored into the shared {@linkplain Graph graph}
	 * ;</li>
	 *
	 * <li>the target container identified by the request item is connected to the newly created resource as required
	 * by the LDP container profile identified by the filtering constraints in the request shape;</li>
	 *
	 * <li>the operation is completed with a {@value Response#Created} status code;</li>
	 *
	 * <li>the IRI of the newly created resource is advertised through the {@code Location} HTTP response header.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is reported with a {@linkplain Response#InternalServerError} status code.</li>
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

		return creator.handle(request);
	}

	/**
	 * Retrieves an LDP resource.
	 *
	 * <p>Handles retrieval requests on the linked data resource identified by the request {@linkplain Request#item()
	 * focus item}, according to the following operating modes.</p>
	 *
	 * <p>If the focus item is a {@linkplain Request#collection() collection}:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is expected to include a combined container/member {@linkplain Shape#shape() shape};</li>
	 *
	 * <li>the response includes the derived shape actually used in the retrieval process;</li>
	 *
	 * <li>the response {@linkplain RDFFormat RDF} body includes the RDF description of the container as matched by the
	 * {@linkplain Guard#Target} area of request shape, linked using the {@code ldp:contains} property to the RDF
	 * description of the resources matched by the {@linkplain Guard#Filter filtering} constrains of the
	 * {@linkplain Guard#Digest} area of the request shape;</li>
	 *
	 * <li>{@linkplain Guard#Digest} area of the request shape doesn't contain any {@linkplain Guard#Filter filtering}
	 * constrains, member resources explicitely to linked to the target container using the {@code ldp:contains}
	 * property are included;</li>
	 *
	 * <li>if the request contains a filtering {@linkplain Request#query(String) query}, only matching container members
	 * descriptions are included.</li>
	 *
	 * <li>if the request contains a {@code Prefer} header requesting the {@code ldp:preferMinimalContainer}
	 * representation, member descriptions are omitted;</li>
	 *
	 * <li>the operation is completed with a {@value Response#OK} status code.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise, if the shared {@linkplain  Graph graph} actually contains a resource matching the request item
	 * IRI:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is expected to include a member resource {@linkplain Shape#shape() shape};</li>
	 *
	 * <li>the response includes the derived shape actually used in the retrieval process;</li>
	 *
	 * <li>the response {@link RDFFormat RDF} body contains the RDF description of the request item, as matched by the
	 * request request shape;</li>
	 *
	 * <li>the operation is completed with a {@value Response#OK} status code.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise:</p>
	 *
	 * <ul>
	 *
	 * <li>the operation is reported as unsuccessful with a {@value Response#NotFound} status code
	 * .</li>
	 *
	 * </ul>
	 *
	 * <p>Regardless of the operating mode, RDF data is retrieved from the shared {@linkplain  Graph graph}.</p>
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

		return relator.handle(request);
	}

	/**
	 * Updates an LDP resource.
	 *
	 * <p>Handles updating requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item}, according to the following operating modes.</p>
	 *
	 * <p>If the request target is a {@linkplain Request#collection() collection}:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is reported with a {@linkplain Response#InternalServerError} status code.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise, if the shared {@linkplain  Graph graph} actually contains a resource matching the request item
	 * IRI:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is expected to include a member resource {@linkplain Shape#shape() shape};</li>
	 *
	 * <li>the request {@link RDFFormat RDF} body is expected to contain an RDF description of the resource to be
	 * updated
	 * matched by the shape;</li>
	 *
	 * <li>the existing RDF description of the target resource matched by the request shape
	 * is replaced in the shared {@linkplain Graph graph} with the request RDF body;</li>
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

		return updater.handle(request);
	}

	/**
	 * Deletes an LDP resource.
	 *
	 * <p>Handles deletion requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item}, according to the following operating modes.</p>
	 *
	 * <p>If the request target is a {@linkplain Request#collection() collection}:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is reported with a {@linkplain Response#InternalServerError} status code.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise, if the shared {@linkplain  Graph graph} actually contains a resource matching the request item
	 * IRI:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is expected to include a member resource {@linkplain Shape#shape() shape};</li>
	 *
	 * <li>the existing RDF description of the target resource matched by the request shape is removed from the shared
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

		return deleter.handle(request);
	}

}
