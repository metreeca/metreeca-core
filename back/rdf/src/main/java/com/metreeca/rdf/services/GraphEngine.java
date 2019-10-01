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
import com.metreeca.rest.services.Logger;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.util.function.Supplier;

import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.services.Graph.graph;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Logger.logger;
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
 * {@linkplain
 * Graph graph}.</p>
 */
public final class GraphEngine implements Engine {

	public static final IRI terms=iri(Values.Internal, "terms");
	public static final IRI stats=iri(Values.Internal, "stats");

	public static final IRI value=iri(Values.Internal, "value");
	public static final IRI type=iri(Values.Internal, "value");

	public static final IRI count=iri(Values.Internal, "count");
	public static final IRI max=iri(Values.Internal, "max");
	public static final IRI min=iri(Values.Internal, "min");


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
	private final Logger logger=service(logger());


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

	@Override public Future<Response> create(final Request request) {

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		//return graph.exec(connection -> {
		//
		//	return Optional.of(resource)
		//
		//			.filter(reserved -> !(
		//					connection.hasStatement(reserved, null, null, true)
		//							|| connection.hasStatement(null, null, reserved, true)
		//			))
		//
		//			.map(reserved -> { // validate before updating graph to support snapshot transactions
		//
		//				final Focus focus=validate(connection, resource, shape, model);
		//
		//				if ( !focus.assess(Issue.Level.Error) ) {
		//
		//					connection.add(anchor(reserved, shape));
		//					connection.add(model);
		//
		//				}
		//
		//				return focus;
		//
		//			});
		//
		//});

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

	//private Iterable<Statement> anchor(final Resource resource, final Shape shape) {
	//	return shape.map(GraphProcessor.filter).map(new Outliner(resource)).collect(toList());
	//}

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
