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

package com.metreeca.form.engines;

import com.metreeca.form.Focus;
import com.metreeca.form.Issue;
import com.metreeca.form.Issue.Level;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;

import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Shape.pass;
import static com.metreeca.form.things.Structures.description;

import static java.util.stream.Collectors.toList;


/**
 * Cell-based query/update engine.
 *
 * <p>Handles CRUD operations on resource cells defined by (labelled) symmetric concise bounded descriptions.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class CellEngine {

	private final RepositoryConnection connection;


	public CellEngine(final RepositoryConnection connection) {

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

		final Model envelope=description(focus, false, model);

		return Focus.focus(model.stream()
				.filter(statement -> !envelope.contains(statement))
				.map(outlier -> issue(Level.Error, "statement outside cell envelope "+outlier, pass()))
				.collect(toList())
		);

	}

}
