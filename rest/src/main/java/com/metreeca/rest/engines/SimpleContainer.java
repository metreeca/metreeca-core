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

import com.metreeca.form.Focus;
import com.metreeca.form.Issue;
import com.metreeca.rest.Engine;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.rest.engines.Descriptions.description;

import static java.util.stream.Collectors.toList;


/**
 * Concise bounded description container engine.
 *
 * <p>Manages CRUD lifecycle operations on (labelled) symmetric concise bounded container descriptions.</p>
 */
final class SimpleContainer implements Engine {

	private final Graph graph;

	private final IRI relation;
	private final boolean inverse;

	private final IRI subject;
	private final Value object;


	SimpleContainer(final Graph graph, final Map<IRI, Value> metadata) {

		this.graph=graph;

		final Value type=metadata.get(RDF.TYPE);
		final Value target=metadata.get(LDP.MEMBERSHIP_RESOURCE);

		final Value direct=metadata.get(LDP.HAS_MEMBER_RELATION);
		final Value inverse=metadata.get(LDP.IS_MEMBER_OF_RELATION);

		if ( direct instanceof IRI ) {

			this.relation=(IRI)direct;
			this.inverse=false;

			this.subject=(target instanceof IRI) ? (IRI)target : LDP.CONTAINER;
			this.object=null;

		} else if ( inverse instanceof IRI ) {

			this.relation=(IRI)inverse;
			this.inverse=true;
			this.subject=null;
			this.object=(target != null) ? target : LDP.CONTAINER;

		} else {

			this.inverse=false;
			this.relation=LDP.CONTAINS;
			this.subject=LDP.CONTAINER;
			this.object=null;

		}

	}


	@Override public Optional<Collection<Statement>> relate(final IRI resource) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Optional<Focus> create(final IRI resource, final IRI related, final Collection<Statement> model) {
		return graph.update(connection -> {

			final Focus focus=validate(related, model);

			if ( !focus.assess(Issue.Level.Error) ) {

				if ( inverse ) {
					connection.add(related, relation, object.equals(LDP.CONTAINER)? resource : object);
				} else {
					connection.add(subject.equals(LDP.CONTAINER)? resource : subject, relation, related);
				}

				connection.add(model);
			}

			return Optional.of(focus);

		});
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {
		throw new UnsupportedOperationException("simple container updating not supported");
	}

	@Override public Optional<IRI> delete(final IRI resource) {
		throw new UnsupportedOperationException("simple container deletion not supported");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Focus validate(final Resource resource, final Collection<Statement> model) {

		final Collection<Statement> envelope=description(resource, false, model);

		return focus(model.stream()
				.filter(statement -> !envelope.contains(statement))
				.map(outlier -> issue(Issue.Level.Error, "statement outside description envelope "+outlier))
				.collect(toList())
		);
	}

}
