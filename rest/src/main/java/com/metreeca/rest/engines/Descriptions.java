/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.engines;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.form.things.Values.pattern;

import static org.eclipse.rdf4j.common.iteration.Iterations.stream;

import static java.util.Collections.singleton;


/**
 * Abstract concise bounded description manager.
 *
 * <p>Manages CRUD lifecycle operations on (labelled) symmetric concise bounded descriptions.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
final class Descriptions {

	/**
	 * Retrieves a symmetric concise bounded description from a statement source.
	 *
	 * @param focus    the resource whose symmetric concise bounded description is to be retrieved
	 * @param labelled if {@code true}, the retrieved description will be extended with {@code rdf:type} and {@code
	 *                 rdfs:label/comment} annotations for all referenced IRIs
	 * @param model    the statement source the description is to be retrieved from
	 *
	 * @return the symmetric concise bounded description of {@code focus} retrieved from {@code model}
	 *
	 * @throws NullPointerException if either {@code focus} or {@code model} is null
	 */
	public static Collection<Statement> description(final Resource focus, final boolean labelled, final Collection<Statement> model) {
		return description(focus, labelled, (s, p, o) -> model.stream().filter(pattern(s, p, o)));
	}

	/**
	 * Retrieves the symmetric concise bounded description of a resource from a repository.
	 *
	 * @param focus      the resource whose symmetric concise bounded description is to be retrieved
	 * @param labelled   if {@code true}, the retrieved description will be extended with {@code rdf:type} and {@code
	 *                   rdfs:label/comment} annotations for all referenced IRIs
	 * @param connection the connection to the repository the description is to be retrieved from
	 *
	 * @return the symmetric concise bounded description of {@code focus} retrieved from {@code connection}
	 *
	 * @throws NullPointerException if either {@code focus} or {@code connection} is null
	 */
	public static Collection<Statement> description(final Resource focus, final boolean labelled, final RepositoryConnection connection) {

		// !!! optimize for SPARQL

		return description(focus, labelled, (s, p, o) -> stream(connection.getStatements(s, p, o, true)));
	}


	private static Collection<Statement> description(final Resource focus, final boolean labelled, final Source source) {

		final Model description=new LinkedHashModel();

		final Queue<Value> pending=new ArrayDeque<>(singleton(focus));
		final Collection<Value> visited=new HashSet<>();

		while ( !pending.isEmpty() ) {

			final Value value=pending.remove();

			if ( visited.add(value) ) {
				if ( value.equals(focus) || value instanceof BNode ) {

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Descriptions() {} // utility


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@FunctionalInterface private static interface Source {

		public Stream<Statement> match(final Resource subject, final IRI predicate, final Value object);

	}

}
