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

package com.metreeca.rest.handlers.work.wrappers;

import com.metreeca.form.Shape;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Traverser;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Sets;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Wrapper;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.function.UnaryOperator;

import static com.metreeca.form.Shape.empty;
import static com.metreeca.form.Shape.pass;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.Option.option;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Sets.set;

import static java.util.stream.Collectors.toList;


/**
 * Shape splitter.
 */
public final class Splitter implements Wrapper {

	private static final Set<IRI> ContainerMetadata=set(
			RDF.TYPE,
			LDP.MEMBERSHIP_RESOURCE,
			LDP.HAS_MEMBER_RELATION,
			LDP.IS_MEMBER_OF_RELATION,
			LDP.INSERTED_CONTENT_RELATION
	);


	public static UnaryOperator<Shape> container() {
		return shape -> {

			final Shape container=shape
					.map(new ContainerTraverser())
					.map(new Optimizer());

			final Shape resource=shape
					.map(new ResourceTraverser())
					.map(new Optimizer());

			return empty(resource) ? pass() : container;

		};
	}

	public static UnaryOperator<Shape> resource() {
		return shape -> {

			final Shape container=shape
					.map(new ContainerTraverser())
					.map(new Optimizer());

			final Shape resource=shape
					.map(new ResourceTraverser())
					.map(new Optimizer());

			final List<Meta> metadata=fields(container) // convert container LDP properties to resource annotations
					.entrySet().stream()
					.filter(entry -> ContainerMetadata.contains(entry.getKey()))
					.map(entry -> entry(entry.getKey(), all(entry.getValue()).orElseGet(Sets::set)))
					.filter(entry -> entry.getValue().size() == 1)
					.map(entry -> meta(entry.getKey(), entry.getValue().iterator().next()))
					.collect(toList());

			return empty(resource) ? container : and(resource, and(metadata)).map(new Optimizer());

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final UnaryOperator<Shape> splitter;

	private final Map<Shape, Shape> cache=new IdentityHashMap<>();


	public Splitter(final UnaryOperator<Shape> splitter) {

		if ( splitter == null ) {
			throw new NullPointerException("null splitter");
		}

		this.splitter=splitter;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> handler.handle(request.shape(
				cache.computeIfAbsent(request.shape(), splitter)
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ContainerTraverser extends Traverser<Shape> {

		@Override public Shape probe(final Shape shape) {
			return shape;
		}


		@Override public Shape probe(final Field field) {
			return field.getIRI().equals(LDP.CONTAINS) ? and() : field;
		}


		@Override public Shape probe(final And and) {
			return and(and.getShapes().stream().map(s -> s.map(this)).collect(toList()));
		}

		@Override public Shape probe(final Or or) {
			return or(or.getShapes().stream().map(s -> s.map(this)).collect(toList()));
		}

		@Override public Shape probe(final Option option) {
			return option(
					option.getTest(),
					option.getPass().map(this),
					option.getFail().map(this)
			);
		}

	}

	private static class ResourceTraverser extends Traverser<Shape> {

		@Override public Shape probe(final Shape shape) {
			return and();
		}


		@Override public Shape probe(final Field field) {
			return field.getIRI().equals(LDP.CONTAINS) ? field.getShape() : and();
		}


		@Override public Shape probe(final And and) {
			return and(and.getShapes().stream().map(s -> s.map(this)).collect(toList()));
		}

		@Override public Shape probe(final Or or) {
			return or(or.getShapes().stream().map(s -> s.map(this)).collect(toList()));
		}

		@Override public Shape probe(final Option option) {
			return option(
					option.getTest(),
					option.getPass().map(this),
					option.getFail().map(this)
			);
		}

	}
}
