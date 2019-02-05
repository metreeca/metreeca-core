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
import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.rest.Engine;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.Optional;

import static com.metreeca.form.Shape.pass;
import static com.metreeca.form.things.Values.iri;


public final class GraphEngine implements Engine {

	public static final IRI meta=iri(Form.Namespace, "meta"); // !!! remove


	private final Engine resource;
	private final Engine container;


	public GraphEngine(final Graph graph, final Shape shape) {

		if ( graph == null ) {
			throw new NullPointerException("null graph");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		final boolean simple=pass(shape);

		this.resource=simple ? new SimpleResource(graph) : new ShapedResource(graph, shape);
		this.container=simple ? new SimpleContainer(graph) : new ShapedContainer(graph, shape);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Collection<Statement>> relate(final IRI resource) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return delegate(resource).relate(resource);
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

		return delegate(resource).create(resource, slug, model);
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return delegate(resource).update(resource, model);
	}

	@Override public Optional<IRI> delete(final IRI resource) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return delegate(resource).delete(resource);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Engine delegate(final Value target) {
		return target.stringValue().endsWith("/")? container : resource;
	}

}
