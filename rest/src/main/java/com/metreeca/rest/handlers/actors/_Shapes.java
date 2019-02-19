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

package com.metreeca.rest.handlers.actors;

import com.metreeca.form.Shape;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Traverser;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Sets;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.Shape.filter;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.shapes.Memoizing.memoizable;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.inverse;

import static java.util.stream.Collectors.toList;


/**
 * Shape utilities.
 */
public final class _Shapes {

	private static final Set<IRI> ContainerMetadata=set(
			RDF.TYPE,
			LDP.MEMBERSHIP_RESOURCE,
			LDP.HAS_MEMBER_RELATION,
			LDP.IS_MEMBER_OF_RELATION,
			LDP.INSERTED_CONTENT_RELATION
	);

	private static final Function<Shape, Shape> entity=memoizable(
			shape -> and(metadata(shape), shape).map(new Optimizer())
	);

	private static final Function<Shape, Shape> container=memoizable(
			merge((container, resource) -> pass(resource) ? pass() : container)
	);

	private static final Function<Shape, Shape> resource=memoizable(
			merge((container, resource) -> pass(resource) ? container : resource)
	);

	private static final Function<Shape, Function<Resource, Shape>> profile=memoizable(shape -> {

		final Map<IRI, Value> metadata=metas(shape);
		final Value type=metadata.get(RDF.TYPE);

		return LDP.BASIC_CONTAINER.equals(type) ? basic()
				: LDP.DIRECT_CONTAINER.equals(type) ? direct(metadata)
				: basic();

	});


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the entity section of a shape.
	 *
	 * @param shape the shape whose entity section is to be retrieved
	 *
	 * @return the input {@code shape}
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static Shape entity(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return shape.map(entity);
	}

	/**
	 * Retrieves the container section of a shape.
	 *
	 * @param shape the shape whose container section is to be retrieved
	 *
	 * @return the input {@code shape} pruned of {@linkplain LDP#CONTAINS ldp:contains} fields, if at least one is
	 * actually included; an empty shape, otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static Shape container(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return shape.map(container);
	}

	/**
	 * Retrieves the resource section of a shape.
	 *
	 * @param shape the shape whose resource section is to be retrieved
	 *
	 * @return the conjunction of the shapes associated to {@linkplain LDP#CONTAINS ldp:contains} fields in the input
	 * {@code shape}, if at least one is actually included; the source {@code shape}, otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static Shape resource(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return shape.map(resource);
	}


	private static Function<Shape, Shape> merge(final BinaryOperator<Shape> merger) {
		return shape -> {

			final Shape container=shape
					.map(new ContainerTraverser())
					.map(new Optimizer());

			final Shape resource=shape
					.map(new ResourceTraverser())
					.map(new Optimizer());

			final Shape metadata=metadata(container);

			// container metadata is added both to container and to resource shape to drive engines

			return merger.apply(and(metadata, container), and(metadata, resource)).map(new Optimizer());

		};
	}

	private static Shape metadata(final Shape shape) {

		final Stream<Meta> metas=metas(shape) // extract existing metadata annotations
				.entrySet().stream()
				.map(entry -> meta(entry.getKey(), entry.getValue()));

		final Stream<Meta> fields=fields(shape) // convert container LDP properties to metadata annotations
				.entrySet().stream()
				.filter(entry -> ContainerMetadata.contains(entry.getKey()))
				.map(entry -> entry(entry.getKey(), all(entry.getValue()).orElseGet(Sets::set)))
				.filter(entry -> entry.getValue().size() == 1)
				.map(entry -> meta(entry.getKey(), entry.getValue().iterator().next()));

		return and(Stream.concat(metas, fields).collect(toList()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape resource(final Resource resource, final Shape shape) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return and(shape, filter().then(all(resource)));
	}

	public static Shape container(final Resource container, final Shape shape) {

		if ( container == null ) {
			throw new NullPointerException("null container");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return and(shape, filter().then(shape.map(profile).apply(container)));
	}


	private static Function<Resource, Shape> basic() {
		return container -> field(inverse(LDP.CONTAINS), container);
	}

	private static Function<Resource, Shape> direct(final Map<IRI, Value> metadata) {

		final Value direct=metadata.get(LDP.HAS_MEMBER_RELATION);
		final Value inverse=metadata.get(LDP.IS_MEMBER_OF_RELATION);
		final Value target=metadata.getOrDefault(LDP.MEMBERSHIP_RESOURCE, LDP.CONTAINER);

		if ( direct != null ) {

			return direct instanceof IRI && target instanceof Resource ?
					target.equals(LDP.CONTAINER)
							? container -> field(inverse((IRI)direct), container)
							: container -> field(inverse((IRI)direct), target)
					: container -> and();

		} else if ( inverse != null ) {

			return inverse instanceof IRI
					? container -> field((IRI)inverse, target.equals(LDP.CONTAINER) ? container : target)
					: container -> and();

		} else {

			return target instanceof Resource
					? container -> field(inverse(LDP.MEMBER), target.equals(LDP.CONTAINER) ? container : (Resource)target)
					: container -> and();

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _Shapes() {} // utility


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

		@Override public Shape probe(final When when) {
			return when(
					when.getTest(),
					when.getPass().map(this),
					when.getFail().map(this)
			);
		}

	}

	private static final class ResourceTraverser extends Traverser<Shape> {

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

		@Override public Shape probe(final When when) {
			return when(
					when.getTest(),
					when.getPass().map(this),
					when.getFail().map(this)
			);
		}

	}

}
