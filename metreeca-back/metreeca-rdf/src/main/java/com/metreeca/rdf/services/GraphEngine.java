/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf.services;

import com.metreeca.rdf.Values;
import com.metreeca.rest.*;
import com.metreeca.rest.services.Engine;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.util.function.Supplier;

import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.services.Graph.graph;
import static com.metreeca.rest.Context.service;
import static com.metreeca.tree.Shape.multiple;
import static com.metreeca.tree.Shape.optional;
import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;


/**
 * Model-driven graph engine.
 *
 * <p>Manages graph transactions and handles model-driven CRUD actions on LDP resources stored in the shared
 * {@linkplain Graph graph}.</p>
 *
 * @see <a href="https://www.w3.org/TR/ldp/">Linked Data Platform 1.0</a>
 */
public final class GraphEngine implements Engine {

	static final String Base=Values.Internal;

	static final IRI terms=iri(Base, "terms");
	static final IRI stats=iri(Base, "stats");

	static final IRI value=iri(Base, "value");
	static final IRI type=iri(Base, "value");

	static final IRI count=iri(Base, "count");
	static final IRI max=iri(Base, "max");
	static final IRI min=iri(Base, "min");


	private static final Shape TermShape=and(
			field(RDFS.LABEL, and(optional(), datatype(XMLSchema.STRING)))
	);

	static final Shape TermsShape=and(
			field(terms, and(multiple(),
					field(value, and(required(), TermShape)),
					field(count, and(required(), datatype(XMLSchema.INTEGER)))
			))
	);

	static final Shape StatsShape=and(

			field(count, and(required(), datatype(XMLSchema.INTEGER))),
			field(min, and(optional(), TermShape)),
			field(max, and(optional(), TermShape)),

			field(stats, and(multiple(),
					field(type, and(required(), datatype(XMLSchema.STRING))),
					field(count, and(required(), datatype(XMLSchema.INTEGER))),
					field(min, and(required(), TermShape)),
					field(max, and(required(), TermShape))
			))

	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=service(graph());

	private final GraphValidator validator=new GraphValidator();
	private final GraphTrimmer trimmer=new GraphTrimmer();

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

	@Override public <M extends Message<M>> Result<M, Failure> trim(final M message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return trimmer.trim(message);
	}

	@Override public <M extends Message<M>> Result<M, Failure> validate(final M message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return validator.validate(message);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/*
	 * Creates an LDP resource.
	 *
	 * <p>Handles creation requests on the linked data container identified by the request {@linkplain Request#item()
	 * focus item}, according to the following operating modes.</p>
	 *
	 * <p>If the request target is a {@linkplain Request#collection() container} and the request includes a resource
	 * {@linkplain Message#shape() shape}:</p>
	 *
	 * <ul>
	 *
	 * <li>the shape is redacted taking into account request user {@linkplain Request#roles() roles}, {@link Shape#Create}
	 * task, {@link Shape#Convey} mode and {@link Shape#Detail} view;</li>
	 *
	 * <li>the request {@link RDFFormat RDF body} is expected to contain an RDF description of the resource to be created
	 * matched by the redacted shape; statements outside this envelope are reported with a {@linkplain
	 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise, If the request target is a {@linkplain Request#collection() container}:</p>
	 *
	 * <ul>
	 *
	 * <li>the request {@link RDFFormat RDF body} is expected to contain a symmetric concise bounded description of the
	 * resource to be created; statements outside this envelope are reported with a {@linkplain
	 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element;</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is reported with a {@linkplain Response#NotImplemented} status code.</li>
	 *
	 * </ul>
	 *
	 * <p>Regardless of the operating mode, the request RDF body is expected to describe the resource to be created using
	 * the request {@linkplain Request#item() focus item} as subject.</p>
	 *
	 * <p>On successful body validation:</p>
	 *
	 * <ul>
	 *
	 * <li>the resource to be created is assigned a unique IRI based on the stem of the {@linkplain Request#stem()
	 * stem} of the request IRI and a name generated by either the default {@linkplain #uuid() UUID-based} or a {@linkplain
	 * #_Creator(BiFunction) custom-provided} slug generator;</li>
	 *
	 * <li>the request RDF body is rewritten to the assigned IRI and stored into the system storage {@linkplain
	 * Engine#engine() engine};</li>
	 *
	 * <li>the target container identified by the request focus item is connected to the newly created resource as required
	 * by the LDP container {@linkplain com.metreeca.tree.things.Shapes profile} identified by {@code rdf:type} and LDP
	 * properties in the request shape.</li>
	 *
	 * </ul>
	 *
	 * <p>On successful resource creation, the IRI of the newly created resource is advertised through the {@code Location}
	 * HTTP response header.</p>
	 *
	 * @param request the request to be handled
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to {@code request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	@Override public Future<Response> create(final Request request) { // !!! tbd

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return creator.handle(request);
	}

	/*
	 * LDP resource relator.
	 *
	 * <p>Handles retrieval requests on the linked data resource identified by the request {@linkplain Request#item()
	 * focus item}, according to the following operating modes.</p>
	 *
	 * <p>If the focus item is a {@linkplain Request#collection() container} and the request includes an expected
	 * {@linkplain Request#shape() shape}:</p>
	 *
	 * <ul>
	 *
	 * <li>the response includes the derived shape actually used in the retrieval process, redacted according to request
	 * user {@linkplain Request#roles() roles}, {@link Shape#relate} task, {@link Shape#convey} mode and {@link Shape#digest}
	 * view;</li>
	 *
	 * <li>the response {@linkplain RDFBody RDF body} includes the RDF description of the container as matched by the
	 * {@linkplain Shapes#container(Shape) container section} of redacted shape, linked using the {@code ldp:contains}
	 * property to the RDF description of the container items matched by the {@linkplain Shapes#resource(Shape) resource
	 * section} of redacted shape;</li>
	 *
	 * <li>contained items are selected as required by the LDP container {@linkplain com.metreeca.tree.things.Shapes
	 * profile} identified by {@code rdf:type} and LDP properties in the request shape;</li>
	 *
	 * <li>if the request contains a filtering {@linkplain Request#query(Shape) query}, only matching container item
	 * descriptions are included.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise, if the focus item is a {@linkplain Request#collection() container}:</p>
	 *
	 * <ul>
	 *
	 * <li>the response {@linkplain RDFBody RDF body} includes the symmetric concise bounded description of the
	 * container, linked using the {@code ldp:contains} property to the symmetric concise bounded description of the
	 * container items, extended with {@code rdf:type} and {@code rdfs:label/comment} annotations for all referenced
	 * IRIs;</li>
	 *
	 * <li>contained items are selected handling the target resource as an LDP Basic Container, that is on the basis of the
	 * {@code ldp:contains} property.</li>
	 *
	 * </ul>
	 *
	 * <p><strong>Warning</strong> / Filtered browsing isn't yet supported on shape-less container.</p>
	 *
	 * <p>In both cases:</p>
	 *
	 * <ul>
	 *
	 * <li>if the request contains a {@code Prefer} header requesting the {@code ldp:preferMinimalContainer}
	 * representation, item descriptions are omitted.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise, if the request includes an expected {@linkplain Request#shape() shape}:</p>
	 *
	 * <ul>
	 *
	 * <li>the response includes the derived shape actually used in the retrieval process, redacted according to request
	 * user {@linkplain Request#roles() roles}, {@link Shape#relate} task, {@link Shape#detail} view and {@link Shape#convey}
	 * mode;</li>
	 *
	 * <li>the response {@link RDFBody RDF body} contains the RDF description of the request focus, as matched by the
	 * redacted request shape.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise:</p>
	 *
	 * <ul>
	 *
	 * <li>the response {@link RDFBody RDF body} contains the symmetric concise bounded description of the request focus
	 * item, extended with {@code rdf:type} and {@code rdfs:label/comment} annotations for all referenced IRIs;</li>
	 *
	 * </ul>
	 *
	 * <p>Regardless of the operating mode, RDF data is retrieved from the system storage {@linkplain Engine#engine()
	 * engine}.</p>
	 *
	 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
	 */
	@Override public Future<Response> relate(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return relator.handle(request);
	}

	/*
	 * LDP resource updater.
	 *
	 * <p>Handles updating requests on the linked data resource identified by the request {@linkplain Request#item()
	 * focus item}, according to the following operating modes.</p>
	 *
	 * <p>If the request target is a {@linkplain Request#collection() container}:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is reported with a {@linkplain Response#NotImplemented} status code.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise, if the request includes an expected {@linkplain Request#shape() resource shape}:</p>
	 *
	 * <ul>
	 *
	 * <li>the resource shape is extracted and redacted taking into account request user {@linkplain Request#roles()
	 * roles}, {@link Form#update} task, {@link Form#convey} mode and {@link Form#detail} view</li>
	 *
	 * <li>the request {@link RDFBody RDF body} is expected to contain an RDF description of the resource to be updated
	 * matched by the redacted resource shape; statements outside this envelope are reported with a {@linkplain
	 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element.</li>
	 *
	 * <li>on successful body validation, the existing RDF description of the target resource matched by the redacted shape
	 * is replaced with the request RDF body.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise:</p>
	 *
	 * <ul>
	 *
	 * <li>the request {@link RDFBody RDF body} is expected to contain a symmetric concise bounded description of the
	 * resource to be updated; statements outside this envelope are reported with a {@linkplain
	 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element;</li>
	 *
	 * <li>on successful body validation, the existing symmetric concise bounded description of the target resource is
	 * replaced with the request RDF body.</li>
	 *
	 * </ul>
	 *
	 * <p>Regardless of the operating mode, RDF data is updated in the system storage {@linkplain Engine#engine() engine}.</p>
	 *
	 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
	 */
	@Override public Future<Response> update(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return updater.handle(request);
	}

	/*
	 * LDP resource deleter.
	 *
	 * <p>Handles deletion requests on the linked data resource identified by the request {@linkplain Request#item()
	 * focus item}, according to the following operating modes.</p>
	 *
	 * <p>If the request target is a {@linkplain Request#collection() container}:</p>
	 *
	 * <ul>
	 *
	 * <li>the request is reported with a {@linkplain Response#NotImplemented} status code.</li>
	 *
	 * </ul>
	 *
	 * <p>If the request includes an expected {@linkplain Request#shape() resource shape}:</p>
	 *
	 * <ul>
	 *
	 * <li>the shape is redacted taking into account request user {@linkplain Request#roles() roles}, {@link Form#delete}
	 * task, {@link Form#convey} mode and {@link Form#detail} view.</li>
	 *
	 * <li>the existing RDF description of the target resource matched by the redacted shape is deleted.</li>
	 *
	 * </ul>
	 *
	 * <p>Otherwise:</p>
	 *
	 * <ul>
	 *
	 * <li>the existing symmetric concise bounded description of the target resource is deleted.</li>
	 *
	 * </ul>
	 *
	 * <p>Regardless of the operating mode, RDF data is removed from the system storage {@linkplain Engine#engine()
	 * engine}.</p>
	 *
	 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
	 */
	@Override public Future<Response> delete(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return deleter.handle(request);
	}

}
