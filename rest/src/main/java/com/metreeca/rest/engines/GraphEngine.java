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
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.rest.Result;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.tray.Tray.tool;


/**
 * Graph-based engine.
 *
 * <p>Manages CRUD operations on linked data resources stored in a {@linkplain Graph graph}.</p>
 */
public final class GraphEngine implements Engine {

	private final Graph graph=tool(Graph.Factory);

	private final Engine resource;
	private final Engine container;


	public GraphEngine(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		final boolean simple=pass(shape); // ignore metadata

		final Map<IRI, Value> metadata=metas(shape);

		this.resource=simple ? new SimpleResource(graph, metadata) : new ShapedResource(graph, shape);
		this.container=simple ? new SimpleContainer(graph, metadata) : new ShapedContainer(graph, shape);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Collection<Statement> relate(final IRI resource) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return delegate(resource).relate(resource);
	}

	@Override public <V, E> Result<V, E> relate(final IRI resource,
			final Function<Shape, Result<? extends Query, E>> parser, final BiFunction<Shape, Collection<Statement>, V> mapper
	) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( parser == null ) {
			throw new NullPointerException("null parser");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return delegate(resource).relate(resource, parser, mapper);
	}

	@Override public Collection<Statement> browse(final IRI resource) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return delegate(resource).browse(resource);
	}

	@Override public <V, E> Result<V, E> browse(final IRI resource,
			final Function<Shape, Result<? extends Query, E>> parser, final BiFunction<Shape, Collection<Statement>, V> mapper
	) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( parser == null ) {
			throw new NullPointerException("null parser");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return delegate(resource).browse(resource, parser, mapper);
	}

	@Override public Optional<Focus> create(final IRI resource, final IRI related, final Collection<Statement> model) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( related == null ) {
			throw new NullPointerException("null slug");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return delegate(resource).create(resource, related, model);
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
		return target.stringValue().endsWith("/") ? container : resource;
	}

}
