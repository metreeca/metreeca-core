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
import com.metreeca.form.Shape;
import com.metreeca.rest.Engine;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;

import static com.metreeca.form.Shape.pass;


public final class GraphEngine implements Engine {

	private final Engine delegate;


	public GraphEngine(final Graph graph, final Shape shape) {

		if ( graph == null ) {
			throw new NullPointerException("null graph");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		// !!! container vs resource

		delegate=pass(shape) ? new SimpleResource(graph) : new ShapedResource(graph, shape);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Collection<Statement>> relate(final IRI resource) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return delegate.relate(resource);
	}

	@Override public Optional<Focus> create(final IRI resource, final IRI slug, final Collection<Statement> model) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( slug == null ) {
			throw new NullPointerException("null slug");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return delegate.create(resource, slug, model);
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return delegate.update(resource, model);
	}

	@Override public Optional<IRI> delete(final IRI resource) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return delegate.delete(resource);
	}

}
