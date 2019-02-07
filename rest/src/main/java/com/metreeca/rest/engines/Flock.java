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

import com.metreeca.form.Shape;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.things.Values.inverse;

import static org.eclipse.rdf4j.common.iteration.Iterations.stream;


/**
 * Container items manager.
 *
 * <p>Manages containment/membership triples for LDP containers.</p>
 */
interface Flock { // !!! decouple from Repository

	public static Optional<Flock> flock(final Map<IRI, Value> metadata) {

		final Value type=metadata.get(LDP.CONTAINER);

		return LDP.BASIC_CONTAINER.equals(type) ? Optional.of(new Basic())
				: LDP.DIRECT_CONTAINER.equals(type) ? Optional.of(new Direct(metadata))
				: Optional.empty();

	}


	public Shape anchor(final IRI resource);

	public Stream<Resource> items(final RepositoryConnection connection, final Resource container); // !!! remove after merging simple/shaped entities

	public RepositoryConnection insert(final RepositoryConnection connection,
			final Resource container, final Resource resource, final Collection<Statement> model
	);

	public RepositoryConnection remove(final RepositoryConnection connection,
			final Resource resource, final Collection<Statement> model
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	final class None implements Flock {

		@Override public Shape anchor(final IRI resource) {
			return and();
		}

		@Override public Stream<Resource> items(final RepositoryConnection connection, final Resource container) {
			return Stream.empty();
		}


		@Override public RepositoryConnection insert(final RepositoryConnection connection,
				final Resource container, final Resource resource, final Collection<Statement> model
		) {

			return connection;

		}

		@Override public RepositoryConnection remove(final RepositoryConnection connection,
				final Resource resource, final Collection<Statement> model
		) {

			return connection;

		}

	}

	final class Basic implements Flock {

		@Override public Shape anchor(final IRI resource) {
			return field(inverse(LDP.CONTAINS), resource);
		}

		@Override public Stream<Resource> items(final RepositoryConnection connection, final Resource container) {
			return stream(connection.getStatements(container, LDP.CONTAINS, null, true))
					.map(Statement::getObject)
					.filter(value -> value instanceof Resource)
					.map(value -> (Resource)value);
		}

		@Override public RepositoryConnection insert(final RepositoryConnection connection,
				final Resource container, final Resource resource, final Collection<Statement> model
		) {

			connection.add(container, LDP.CONTAINS, resource);

			return connection;

		}

		@Override public RepositoryConnection remove(final RepositoryConnection connection,
				final Resource resource, final Collection<Statement> model
		) {

			connection.remove((Resource)null, LDP.CONTAINS, resource);

			return connection;

		}

	}

	final class Direct implements Flock {

		private final IRI relation;

		private final Resource subject;
		private final Value object;


		Direct(final Map<IRI, Value> metadata) {

			final Value direct=metadata.get(LDP.HAS_MEMBER_RELATION);
			final Value inverse=metadata.get(LDP.IS_MEMBER_OF_RELATION);

			final Value target=metadata.get(LDP.MEMBERSHIP_RESOURCE);

			if ( direct != null ) {

				this.relation=(direct instanceof IRI) ? (IRI)direct : null;
				this.subject=(target == null) ? LDP.CONTAINER : (target instanceof Resource) ? (Resource)target : null;
				this.object=null;

			} else if ( inverse != null ) {

				this.relation=(inverse instanceof IRI) ? (IRI)inverse : null;
				this.subject=null;
				this.object=(target == null) ? LDP.CONTAINER : target;

			} else {

				this.relation=LDP.MEMBER;
				this.subject=(target == null) ? LDP.CONTAINER : (target instanceof Resource) ? (Resource)target : null;
				this.object=null;

			}

		}

		@Override public Shape anchor(final IRI resource) {
			return relation != null && subject != null ? field(inverse(relation), subject)
					: relation != null && object != null ? field(relation, object)
					: and();
		}

		@Override public Stream<Resource> items(final RepositoryConnection connection, final Resource container) {
			return relation != null && subject != null ?

					stream(connection.getStatements(subject(container), relation, null, true))
							.map(Statement::getObject)
							.filter(value -> value instanceof Resource)
							.map(value -> (Resource)value)

					: relation != null && object != null ?

					stream(connection.getStatements(null, relation, object(container), true))
							.map(Statement::getSubject)

					: Stream.empty();
		}

		@Override public RepositoryConnection insert(final RepositoryConnection connection,
				final Resource container, final Resource resource, final Collection<Statement> model
		) {

			if ( relation != null && subject != null ) {
				connection.add(subject(container), relation, resource);
			}

			if ( relation != null && object != null ) {
				connection.add(resource, relation, object(container));
			}

			return connection;
		}

		@Override public RepositoryConnection remove(final RepositoryConnection connection,
				final Resource resource, final Collection<Statement> model
		) {

			if ( relation != null && subject != null ) {
				connection.remove((Resource)null, relation, resource);
			}

			if ( relation != null && object != null ) {
				connection.remove(resource, relation, null);
			}

			return connection;
		}


		private Resource subject(final Resource container) {
			return subject.equals(LDP.CONTAINER) ? container : subject;
		}

		private Value object(final Value container) {
			return object.equals(LDP.CONTAINER) ? container : object;
		}

	}

}
