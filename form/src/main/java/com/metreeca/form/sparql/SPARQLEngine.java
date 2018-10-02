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

package com.metreeca.form.sparql;

import com.metreeca.form.*;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.shapes.All;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.Report.report;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Lists.concat;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;


public final class SPARQLEngine { // !!! migrate from utility class to processor (new SPARQLEngine(connection))

	public static boolean transactional(final RepositoryConnection connection) {
		return !connection.getIsolationLevel().equals(IsolationLevels.NONE);
	}

	public static boolean contains(final RepositoryConnection connection, final Resource resource) {

		// identify and ignore housekeeping historical references (e.g. versioning/auditing)
		// !!! support returning 410 Gone if the resource is known to have existed (as identified by housekeeping)
		// !!! optimize using a single query if working on a remote repository

		return connection.hasStatement(resource, null, null, true)
				|| connection.hasStatement(null, null, resource, true);
	}


	public static Map<Value, Collection<Statement>> browse(final RepositoryConnection connection, final Shape shape) {
		return browse(connection, new Edges(shape));
	}

	public static Map<Value, Collection<Statement>> browse(final RepositoryConnection connection, final Query query) {
		return new SPARQLReader(connection).process(query);
	}


	public static Collection<Statement> relate(
			final RepositoryConnection connection, final IRI focus, final Shape shape) {
		return new SPARQLReader(connection)
				.process(new Edges(and(All.all(focus), shape)))
				.entrySet()
				.stream()
				.findFirst()
				.map(Map.Entry::getValue)
				.orElseGet(Collections::emptySet);
	}

	public static Report create(
			final RepositoryConnection connection, final IRI focus, final Shape shape, final Collection<Statement> model) {

		final boolean transactional=transactional(connection);

		// upload statements to repository and validate against shape
		// disable shape-driven validation if not transactional // !!! just downgrade

		final Report report=new SPARQLWriter(connection).process(transactional ? shape : and(), model, focus);

		// validate shape envelope // !!! validate even if not transactional

		final Collection<Statement> envelope=report.outline();

		final Collection<Statement> outliers=transactional ? model.stream()
				.filter(statement -> !envelope.contains(statement))
				.collect(toList()) : emptySet();

		// extend validation report with statements outside shape envelope

		return outliers.isEmpty() ? report : report(concat(report.getIssues(), outliers.stream()
				.map(outlier -> issue(Issue.Level.Error, "unexpected statement "+outlier, shape))
				.collect(toList())
		), report.getFrames());
	}

	public static Report update(
			final RepositoryConnection connection, final IRI focus, final Shape shape, final Collection<Statement> model) {

		// !!! merge retrieve/remove/insert operations into a single SPARQL update txn

		delete(connection, focus, shape); // identify and remove updatable cell

		return create(connection, focus, shape, model); // upload and validate submitted statements
	}

	public static void delete(
			final RepositoryConnection connection, final IRI focus, final Shape shape) {

		// !!! merge retrieve/remove operations into a single SPARQL update txn

		final Collection<Statement> model=relate(connection, focus, shape); // identify and remove deletable cell

		connection.remove(model);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private SPARQLEngine() {}

}
