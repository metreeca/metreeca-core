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

import com.metreeca.form.*;
import com.metreeca.form.queries.Edges;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.form.things.Values.rewrite;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * Shape-driven SPARQL query/update engine.
 */
public final class SPARQLEngine {

	public static boolean transactional(final RepositoryConnection connection) {
		return !connection.getIsolationLevel().equals(IsolationLevels.NONE);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final RepositoryConnection connection;


	public SPARQLEngine(final RepositoryConnection connection) {

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		this.connection=connection;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Map<Resource, Collection<Statement>> browse(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return browse(new Edges(shape));
	}

	public Map<Resource, Collection<Statement>> browse(final Query query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return new SPARQLReader(connection).process(query);
	}

	public Collection<Statement> browse(final Query query, final IRI focus) { // !!! review/remove

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return browse(query).values().stream()
				.flatMap(statements -> statements.stream().map(statement -> rewrite(Form.meta, focus, statement)))
				.collect(toList());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Collection<Statement> relate(final IRI focus, final Shape shape) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return new SPARQLReader(connection)
				.process(new Edges(and(all(focus), shape)))
				.entrySet()
				.stream()
				.findFirst()
				.map(Map.Entry::getValue)
				.orElseGet(Collections::emptySet);
	}

	public Focus create(final IRI focus, final Shape shape, final Collection<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		final boolean transactional=transactional(connection);

		// upload statements to repository and validate against shape
		// disable shape-driven validation if not transactional // !!! just downgrade

		final Focus report=new SPARQLWriter(connection).process(transactional ? shape : and(), model, focus);

		// validate shape envelope // !!! validate even if not transactional

		final Collection<Statement> envelope=report.outline().collect(toSet());

		final Collection<Statement> outliers=transactional ? model.stream()
				.filter(statement -> !envelope.contains(statement))
				.collect(toList()) : emptySet();

		// extend validation report with statements outside shape envelope

		return outliers.isEmpty() ? report : focus(concat(report.getIssues(), outliers.stream()
				.map(outlier -> issue(Issue.Level.Error, "statement outside shape envelope "+outlier, shape))
				.collect(toList())
		), report.getFrames());
	}

	public Focus update(final IRI focus, final Shape shape, final Collection<Statement> model) {

		// !!! merge retrieve/remove/insert operations into a single SPARQL update txn

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		delete(focus, shape); // identify and remove updatable cell

		return create(focus, shape, model); // upload and validate submitted statements
	}

	public void delete(final IRI focus, final Shape shape) {

		// !!! merge retrieve/remove operations into a single SPARQL update txn

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		final Collection<Statement> model=relate(focus, shape); // identify and remove deletable cell

		connection.remove(model);
	}

}
