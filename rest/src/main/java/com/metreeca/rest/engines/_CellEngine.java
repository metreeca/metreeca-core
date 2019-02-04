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
import com.metreeca.form.Issue.Level;
import com.metreeca.rest.Engine;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Optional;

import static com.metreeca.form.Issue.issue;
import static com.metreeca.rest.engines.Descriptions.description;

import static java.util.stream.Collectors.toList;


/**
 * Cell-based query/update engine.
 *
 * <p>Handles CRUD operations on resource cells defined by (labelled) symmetric concise bounded descriptions.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class _CellEngine implements Engine {

	@Override public Optional<Collection<Statement>> relate(final IRI resource) throws UnsupportedOperationException {
		return null;
	}

	@Override public Optional<Focus> create(final IRI resource, final IRI slug, final Collection<Statement> model) throws UnsupportedOperationException {
		return null;
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) throws UnsupportedOperationException {
		return null;
	}

	@Override public Optional<IRI> delete(final IRI resource) throws UnsupportedOperationException {
		return Optional.empty();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final RepositoryConnection connection;


	public _CellEngine(final RepositoryConnection connection) {

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		this.connection=connection;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a resource cell.
	 *
	 * @param focus the focus resource for the cell to be retrieved
	 * @return the labelled symmetric bounded description of {@code focus}
	 *
	 * @throws NullPointerException if {@code focus} is null
	 */
	public Collection<Statement> relate(final Resource focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return description(focus, true, connection);
	}

	/**
	 * Creates a resource cell.
	 *
	 * @param focus the focus resource for the cell to be created
	 * @param model the statements to be included in the newly created cell centered on {@code focus}
	 *
	 * @return a validation report for the operation; includes {@linkplain Issue.Level#Error errors} if {@code model}
	 * contains statements outside the symmetric concise bounded description of {@code focus}
	 *
	 * @throws NullPointerException if either {@code focus} or {@code model} is null
	 */
	public Focus create(final Resource focus, final Collection<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		final Focus report=validate(focus, model);

		if ( !report.assess(Level.Error) ) {

			connection.add(model);

		}

		return report;
	}

	/**
	 * Updates a resource cell.
	 *
	 * @param focus the focus resource for the cell to be updated
	 * @param model the statements to be included in the updated cell centered on {@code focus}
	 *
	 * @return a validation report for the operation; includes {@linkplain Issue.Level#Error errors} if {@code model}
	 * contains statements outside the symmetric concise bounded description of {@code focus}
	 *
	 * @throws NullPointerException if either {@code focus} or {@code model} is null
	 */
	public Focus update(final Resource focus, final Collection<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		final Focus report=validate(focus, model);

		if ( !report.assess(Level.Error) ) {

			connection.remove(description(focus, false, connection));
			connection.add(model);

		}

		return report;
	}

	/**
	 * Deletes a resource cell.
	 *
	 * @param focus the focus resource for the cell to be deleted
	 *
	 * @throws NullPointerException if {@code focus} is null
	 */
	public void delete(final Resource focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		connection.remove(description(focus, false, connection));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Focus validate(final Resource focus, final Collection<Statement> model) {

		final Collection<Statement> envelope=description(focus, false, model);

		return Focus.focus(model.stream()
				.filter(statement -> !envelope.contains(statement))
				.map(outlier -> issue(Level.Error, "statement outside cell envelope "+outlier))
				.collect(toList())
		);

	}

}
