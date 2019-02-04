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

package com.metreeca.rest.flavors;

import com.metreeca.form.Focus;
import com.metreeca.form.Shape;
import com.metreeca.form.queries.Edges;
import com.metreeca.rest.Flavor;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.*;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;


public final class ShapedResource implements Flavor {

	private final RepositoryConnection connection;
	private final Shape shape;


	public ShapedResource(final RepositoryConnection connection, final Shape shape) {

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.connection=connection;
		this.shape=shape;
	}



	@Override public Optional<Collection<Statement>> relate(final IRI entity) {

		if ( entity == null ) {
			throw new NullPointerException("null entity");
		}

		return new SPARQLReader(connection)
				.process(new Edges(and(all(entity), shape)))
				.entrySet()
				.stream()
				.findFirst()
				.map(Map.Entry::getValue);

	}

	@Override public Focus create(final IRI entity, final Collection<Statement> model) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Focus update(final IRI entity, final Collection<Statement> model) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public boolean delete(final IRI entity) {

		// !!! merge retrieve/remove operations into a single SPARQL update txn

		if ( entity == null ) {
			throw new NullPointerException("null entity");
		}

		final Optional<Collection<Statement>> model=relate(entity); // identify deletable cell

		model.ifPresent(statements -> connection.remove(statements));

		return model.isPresent();
	}

}
