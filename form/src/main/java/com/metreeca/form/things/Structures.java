/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.form.things;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.*;

import static java.util.Collections.singleton;


/**
 * RDF structure utilities.
 *
 * <p>Provides utility methods for retrieving RDF structures like lists and descriptions from RDF models, repositories
 * and other statement sources.</p>*
 *
 * @see <a href="https://www.w3.org/TR/rdf-schema/#ch_collectionvocab/">RDF Schema 1.1 - § 5.2 RDF Collections</a>
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Structures {

	/**
	 * Retrieves the symmetric concise bounded description of a resource from a repository.
	 *
	 * @param focus      the resource whose symmetric concise bounded description is to be retrieved
	 * @param labelled   if {@code true}, the retrieved description will be extended with {@code rdfs:label/comment}
	 *                   annotations for all referenced IRIs
	 * @param connection the connection to the repository the description is to be retrieved from
	 *
	 * @return the symmetric concise bounded description of {@code focus} retrieved from {@code connection}
	 *
	 * @throws NullPointerException if either {@code focus} or {@code connection} is null
	 */
	public static Model cell(final Resource focus, final boolean labelled, final RepositoryConnection connection) {

		// !!! optimize for SPARQL

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		final Model model=new LinkedHashModel();

		final Queue<Value> pending=new ArrayDeque<>(singleton(focus));
		final Collection<Value> visited=new HashSet<>();

		while ( !pending.isEmpty() ) {

			final Value value=pending.remove();

			if ( visited.add(value) ) {
				if ( value.equals(focus) || value instanceof BNode ) {

					try (final RepositoryResult<Statement> statements=connection.getStatements(
							(Resource)value, null, null, true
					)) {
						while ( statements.hasNext() ) {

							final Statement statement=statements.next();

							model.add(statement);
							pending.add(statement.getObject());
						}
					}

					try (final RepositoryResult<Statement> statements=connection.getStatements(
							null, null, value, true
					)) {
						while ( statements.hasNext() ) {

							final Statement statement=statements.next();

							model.add(statement);
							pending.add(statement.getSubject());
						}
					}

				} else if ( labelled && value instanceof IRI ) {

					try (final RepositoryResult<Statement> statements=connection.getStatements(
							(Resource)value, RDFS.LABEL, null, true
					)) {
						while ( statements.hasNext() ) { model.add(statements.next()); }
					}

					try (final RepositoryResult<Statement> statements=connection.getStatements(
							(Resource)value, RDFS.COMMENT, null, true
					)) {
						while ( statements.hasNext() ) { model.add(statements.next()); }
					}

				}
			}

		}

		return model;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Structures() {} // utility

}
