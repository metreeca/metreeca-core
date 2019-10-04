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
import com.metreeca.rdf._probes.Outliner;
import com.metreeca.rdf.formats.RDFFormat;
import com.metreeca.rest.*;
import com.metreeca.rest.services.Engine;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
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

import static java.util.stream.Collectors.toList;


/**
 * Model-driven graph engine.
 *
 * <p>Manages graph transactions and handles model-driven CRUD actions on LDP resources stored in the shared
 * {@linkplain
 * Graph graph}.</p>
 */
public final class GraphEngine implements Engine {

	static final IRI terms=iri(Values.Internal, "terms");
	static final IRI stats=iri(Values.Internal, "stats");

	static final IRI value=iri(Values.Internal, "value");
	static final IRI type=iri(Values.Internal, "value");

	static final IRI count=iri(Values.Internal, "count");
	static final IRI max=iri(Values.Internal, "max");
	static final IRI min=iri(Values.Internal, "min");


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

	private final GraphCreator creator=new GraphCreator();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <R> R exec(final Supplier<R> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return graph.exec(connection -> { return task.get(); });
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Shape container(final Shape shape) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Shape resource(final Shape shape) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <M extends Message<M>> Result<M, Failure> trim(final M message) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public <M extends Message<M>> Result<M, Failure> validate(final M message) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * LDP resource creator.
	 *
	 * <p>Handles creation requests on the linked data container identified by the request {@linkplain Request#item() focus
	 * item}, according to the following operating modes.</p>
	 *
	 * <p>If the request target is a {@linkplain Request#container() container} and the request includes a resource
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
	 * <p>Otherwise, If the request target is a {@linkplain Request#container() container}:</p>
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
	 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
	 */
	@Override public Future<Response> create(final Request request) { // !!! tbd

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return creator.handle(request);
	}

	@Override public Future<Response> relate(final Request request) {

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		//return graph.exec(connection -> {
		//	return query.map(new GraphRetriever(trace, connection, resource));
		//});
	}

	@Override public Future<Response> update(final Request request) {

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		//return graph.exec(connection -> {
		//	return retrieve(connection, resource, shape).map(current -> {
		//
		//		// validate against shape before updating graph to support snapshot transactions
		//
		//		final Focus focus=validate(connection, resource, shape, model);
		//
		//		if ( !focus.assess(Issue.Level.Error) ) {
		//			connection.remove(current);
		//			connection.add(model);
		//		}
		//
		//		return focus;
		//
		//	});
		//});
	}

	@Override public Future<Response> delete(final Request request) {

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		//return graph.exec(connection -> {
		//
		//	// !!! merge retrieve/remove operations into a single SPARQL update txn
		//	// !!! must check resource existence anyway and wouldn't work for CBD shapes
		//
		//	return retrieve(connection, resource, shape).map(current -> {
		//
		//		connection.remove(anchor(resource, shape));
		//		connection.remove(current);
		//
		//		return focus(set(), set(frame(resource)));
		//
		//	});
		//
		//});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//private Optional<Collection<Statement>> retrieve(
	//		final RepositoryConnection connection, final IRI resource, final Shape shape
	//) {
	//	return Optional.of(edges(shape))
	//
	//			.map(query -> query.map(new GraphRetriever(trace, connection, resource)))
	//
	//			.filter(current -> !current.isEmpty());
	//}

	//private Focus validate(final RepositoryConnection connection,
	//		final Resource resource, final Shape shape, final Collection<Statement> model
	//) {
	//
	//	final Shape target=shape.map(GraphProcessor.convey);
	//
	//	final Focus focus=target // validate against shape
	//			.map(new GraphValidator(trace, connection, set(resource), model));
	//
	//	final Collection<Statement> envelope=pass(target)
	//			? description(resource, false, model) // collect resource cbd
	//			: focus.outline().collect(toSet()); // collect shape envelope
	//
	//	return focus( // extend validation report with errors for statements outside shape envelope
	//
	//			Stream.concat(
	//
	//					focus.getIssues().stream(),
	//
	//					model.stream().filter(statement -> !envelope.contains(statement)).map(outlier ->
	//							issue(Issue.Level.Error, "statement outside shape envelope "+outlier)
	//					)
	//
	//			).collect(toList()),
	//
	//			focus.getFrames()
	//
	//	);
	//}

}
