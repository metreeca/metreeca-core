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
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;


/**
 * Container items manager.
 *
 * <p>Manages containment/membership triples for LDP containers.</p>
 */
interface Flock {

	public static Optional<Flock> flock(final Map<IRI, Value> metadata) {

		final Value type=metadata.get(RDF.TYPE);

		return LDP.BASIC_CONTAINER.equals(type) ? Optional.of(new Basic())
				: LDP.DIRECT_CONTAINER.equals(type) ? Optional.of(new Direct(metadata))
				: Optional.empty();

	}


	public RepositoryConnection insert(final RepositoryConnection connection,
			final IRI container, final IRI resource, final Collection<Statement> model
	);

	public RepositoryConnection remove(final RepositoryConnection connection,
			final IRI resource, final Collection<Statement> model
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	final class Basic implements Flock {

		@Override public RepositoryConnection insert(final RepositoryConnection connection,
				final IRI container, final IRI resource, final Collection<Statement> model
		) {

			connection.add(container, LDP.CONTAINS, resource);

			return connection;

		}

		@Override public RepositoryConnection remove(final RepositoryConnection connection,
				final IRI resource, final Collection<Statement> model
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

				this.relation= LDP.CONTAINS;
				this.subject=(target == null) ? LDP.CONTAINER : (target instanceof Resource) ? (Resource)target : null;
				this.object=null;

			}

		}


		@Override public RepositoryConnection insert(final RepositoryConnection connection,
				final IRI container, final IRI resource, final Collection<Statement> model
		) {

			if ( relation != null && subject != null ) {
				connection.add(subject.equals(LDP.CONTAINER) ? container : subject, relation, resource);
			}

			if ( relation != null && object != null ) {
				connection.add(resource, relation, object.equals(LDP.CONTAINER) ? container : object);
			}

			return connection;
		}

		@Override public RepositoryConnection remove(final RepositoryConnection connection,
				final IRI resource, final Collection<Statement> model
		) {

			if ( relation != null && subject != null ) {
				connection.remove((Resource)null, relation, resource);
			}

			if ( relation != null && object != null ) {
				connection.remove(resource, relation, null);
			}

			return connection;
		}

	}

}
