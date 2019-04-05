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

package com.metreeca.form.things;

import com.metreeca.form.Shape;
import com.metreeca.form.probes.Extractor;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.metreeca.form.things.Values.pattern;

import static org.eclipse.rdf4j.common.iteration.Iterations.stream;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toCollection;


/**
 * RDF structure utilities.
 *
 * <p>Manages retrieval of complex RDF structures from statement sources.</p>
 */
public final class Structures {

	//// Symmetric CBD /////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the symmetric concise bounded description of a resource from a statement source.
	 *
	 * @param resource the resource whose symmetric concise bounded description is to be retrieved
	 * @param labelled if {@code true}, the retrieved description will be extended with {@code rdf:type} and {@code
	 *                 rdfs:label/comment} annotations for all referenced IRIs
	 * @param model    the statement source the description is to be retrieved from
	 *
	 * @return the symmetric concise bounded description of {@code resource} retrieved from {@code model}
	 *
	 * @throws NullPointerException if either {@code resource} or {@code model} is null
	 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
	 */
	public static Collection<Statement> description(
			final Resource resource, final boolean labelled, final Iterable<Statement> model
	) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return description(resource, labelled, source(model));
	}

	/**
	 * Retrieves the symmetric concise bounded description of a resource from a repository.
	 *
	 * @param resource   the resource whose symmetric concise bounded description is to be retrieved
	 * @param labelled   if {@code true}, the retrieved description will be extended with {@code rdf:type} and {@code
	 *                   rdfs:label/comment} annotations for all referenced IRIs
	 * @param connection the connection to the repository the description is to be retrieved from
	 *
	 * @return the symmetric concise bounded description of {@code focus} retrieved from {@code connection}
	 *
	 * @throws NullPointerException if either {@code focus} or {@code connection} is null
	 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
	 */
	public static Collection<Statement> description(
			final Resource resource, final boolean labelled, final RepositoryConnection connection
	) {

		// !!! optimize for SPARQL

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		return description(resource, labelled, source(connection));
	}


	private static Collection<Statement> description(
			final Resource resource, final boolean labelled, final Source source
	) {

		final Model description=new LinkedHashModel();

		final Queue<Value> pending=new ArrayDeque<>(singleton(resource));
		final Collection<Value> visited=new HashSet<>();

		while ( !pending.isEmpty() ) {

			final Value value=pending.remove();

			if ( visited.add(value) ) {
				if ( value.equals(resource) || value instanceof BNode ) {

					source.match((Resource)value, null, null)
							.peek(statement -> pending.add(statement.getObject()))
							.forEach(description::add);

					source.match(null, null, value)
							.peek(statement -> pending.add(statement.getSubject()))
							.forEach(description::add);

				} else if ( labelled && value instanceof IRI ) {

					source.match((Resource)value, RDF.TYPE, null).forEach(description::add);
					source.match((Resource)value, RDFS.LABEL, null).forEach(description::add);
					source.match((Resource)value, RDFS.COMMENT, null).forEach(description::add);

				}
			}

		}

		return description;

	}


	//// Network ///////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a reachable network from a statement source.
	 *
	 * @param resource the resource whose reachable network is to be retrieved
	 * @param model    the statement source the reachable network is to be retrieved from
	 *
	 * @return the reachable network of {@code resource} retrieved from {@code model}
	 *
	 * @throws NullPointerException if either {@code resource} or {@code model} is null
	 */
	public static Model network(final Resource resource, final Iterable<Statement> model) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return network(resource, source(model));
	}

	/**
	 * Retrieves a reachable network from a repository.
	 *
	 * @param resource   the resource whose reachable network is to be retrieved
	 * @param connection the connection to the repository the reachable network is to be retrieved from
	 *
	 * @return the reachable network of {@code resource} retrieved from {@code model}
	 *
	 * @throws NullPointerException if either {@code resource} or {@code connection} is null
	 */
	public static Model network(final Resource resource, final RepositoryConnection connection) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		return network(resource, source(connection));
	}


	private static Model network(final Resource resource, final Source source) {

		final Model network=new LinkedHashModel();

		final Queue<Value> pending=new ArrayDeque<>(singleton(resource));
		final Collection<Value> visited=new HashSet<>();

		while ( !pending.isEmpty() ) {

			final Value value=pending.remove();

			if ( visited.add(value) ) {
				if ( value instanceof Resource ) {

					source.match((Resource)value, null, null)
							.peek(statement -> pending.add(statement.getObject()))
							.forEach(network::add);

					source.match(null, null, value)
							.peek(statement -> pending.add(statement.getSubject()))
							.forEach(network::add);

				}

			}

		}

		return network;
	}


	//// Shape Envelope ////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the shape envelope of a resource from a statement source.
	 *
	 * @param resource the resource whose shape envelope is to be retrieved
	 * @param shape    the shape whose envelope is to be retrieved
	 * @param model    the statement source the shape envelope is to be retrieved from
	 *
	 * @return the {@code shape} envelope of {@code resource} retrieved from {@code model}, that is the subset of its
	 * reachable network that is compatible with {@code shape}
	 *
	 * @throws NullPointerException if any argument is null
	 */
	public static Model envelope(final Resource resource, final Shape shape, final Iterable<Statement> model) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return shape.map(new Extractor(model, singleton(resource))).collect(toCollection(LinkedHashModel::new));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Source source(final Iterable<Statement> model) {
		return (s, p, o) -> StreamSupport.stream(model.spliterator(), false).filter(pattern(s, p, o));
	}

	private static Source source(final RepositoryConnection connection) {
		return (s, p, o) -> stream(connection.getStatements(s, p, o, true));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Structures() {} // utility


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@FunctionalInterface private static interface Source {

		public Stream<Statement> match(final Resource subject, final IRI predicate, final Value object);

	}

}
