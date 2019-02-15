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

package com.metreeca.rest.engines_;

import com.metreeca.form.Focus;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;

import static com.metreeca.tray.Tray.tool;


/**
 * Graph-based engine.
 *
 * <p>Manages CRUD operations on linked data resources stored in the system {@linkplain Graph graph}.</p>
 */
public final class GraphEngine {

	private final Graph graph=tool(Graph.Factory);


	public Collection<Statement> relate(final IRI resource, final Query query) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	public Collection<Statement> browse(final IRI resource, final Query query) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}


	public Optional<Focus> create(final IRI resource, final Shape shape, final IRI related, final Collection<Statement> model) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( related == null ) {
			throw new NullPointerException("null slug");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	public Optional<Focus> update(final IRI resource, final Shape shape, final Collection<Statement> model) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	public Optional<Focus> delete(final IRI resource, final Shape shape) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

}
