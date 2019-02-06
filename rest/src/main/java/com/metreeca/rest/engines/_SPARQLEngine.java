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

import com.metreeca.form.Form;
import com.metreeca.form.Query;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Map;

import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.rewrite;

import static java.util.stream.Collectors.toList;


/**
 * Shape-driven SPARQL query/update engine.
 */
public final class _SPARQLEngine {

	public static final IRI meta=iri(Form.Namespace, "meta"); // !!! remove


	public static boolean transactional(final RepositoryConnection connection) {
		return !connection.getIsolationLevel().equals(IsolationLevels.NONE);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final RepositoryConnection connection;


	public _SPARQLEngine(final RepositoryConnection connection) {

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		this.connection=connection;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Map<Resource, Collection<Statement>> browse(final Query query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return new ShapedRetriever(connection).process(query);
	}

	public Collection<Statement> browse(final Query query, final IRI focus) { // !!! review/remove

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return browse(query).values().stream()
				.flatMap(statements -> statements.stream().map(statement -> rewrite(meta, focus, statement)))
				.collect(toList());
	}

}
