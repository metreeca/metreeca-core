/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rdf._engine;

import com.metreeca.tree.shapes.*;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Optimizer;
import com.metreeca.tree.probes.Traverser;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.tree.Shape.filter;
import static com.metreeca.tree.probes.Evaluator.pass;
import static com.metreeca.tree.shapes.Field.fields;
import static com.metreeca.tree.shapes.Memoizing.*;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;
import static com.metreeca.tree.things.Maps.entry;
import static com.metreeca.tree.things.Sets.set;
import static com.metreeca.rdf.Values.inverse;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Meta.meta;
import static com.metreeca.tree.shapes.Meta.metas;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static java.util.stream.Collectors.toList;

;


/**
 * Shape utilities.
 *
 * <p>Supports splitting/merging of combo shapes describing LDP containers and associated resources.</p>
 *
 * <p>In combo shapes a <em>container</em> shape is connected to a set of <em>resource</em> shapes through {@code
 * ldp:contains} {@linkplain Field fields}.</p>
 *
 * <p>The LDP profile of the container is identified by its {@code rdf:type} and LDP properties, as inferred either
 * from {@linkplain Meta metadata} annotations or {@linkplain Field field} constraints in the combo shape, defaulting to
 * the <em>Basic</em> profile if no metadata is available:</p>
 *
 * <table summary="container profiles">
 *
 * <tr>
 * <th>{@code rdf:type}</th>
 * <th>container profile</th>
 * <th>container properties</th>
 * </tr>
 *
 * <tr>
 * <td>{@code ldp:BasicContainer}</td>
 * <td><a href="https://www.w3.org/TR/ldp/#ldpbc">Basic</a></td>
 * <td>–</td>
 * </tr>
 *
 * <tr>
 * <td>{@code ldp:DirectContainer}</td>
 * <td><a href="https://www.w3.org/TR/ldp/#ldpdc">Direct</a></td>
 * <td><ul>
 * <li>{@code ldp:hasMemberRelation}</li>
 * <li>{@code ldp:isMemberOfRelation}</li>
 * <li>{@code ldp:membershipResource}</li>
 * </ul></td>
 * </tr>
 *
 * <tr>
 * <td>{@code ldp:IndirectContainer}</td>
 * <td><a href="https://www.w3.org/TR/ldp/#ldpic">Indirect</a> </td>
 * <td><ul>
 * <li><em>all Direct properties</em></li>
 * <li>{@code ldp:insertedContentRelation}</li>
 * </ul></td>
 * </tr>
 *
 * </table>
 *
 * <p><strong>Warning</strong> / Only Basic/Direct profiles are currently supported.</p>
 *
 * @see <a href="https://www.w3.org/TR/ldp/">Linked Data Platform 1.0</a>
 */
public final class Shapes {

	/**
	 * Splits resource / nested container IRI components.
	 */
	private static final Pattern ResourcePattern=Pattern.compile("^(?<resource>.*)(?<nested>/[^/]+/?)$");


	private static final Set<IRI> ContainerMetadata=set(
			RDF.TYPE,
			LDP.MEMBERSHIP_RESOURCE,
			LDP.HAS_MEMBER_RELATION,
			LDP.IS_MEMBER_OF_RELATION,
			LDP.INSERTED_CONTENT_RELATION
	);

	private static final Function<Shape, Shape> entity=(
			shape -> and(metadata(shape), shape).map(new _Optimizer())
	);

	private static final Function<Shape, Shape> container=(
			merge((container, resource) -> pass(resource) ? pass() : container)
	);

	private static final Function<Shape, Shape> resource=(
			merge((container, resource) -> pass(resource) ? container : resource)
	);

	private static final Function<Shape, Function<Resource, Shape>> profile=(shape -> {

		final Map<IRI, Value> metadata=metas(shape);
		final Value type=metadata.get(RDF.TYPE);

		return LDP.BASIC_CONTAINER.equals(type) ? basic()
				: LDP.DIRECT_CONTAINER.equals(type) ? direct(metadata)
				: basic();

	});


	//// Splitters /////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the entity section of a shape.
	 *
	 * @param shape the shape whose entity section is to be retrieved
	 *
	 * @return the input {@code shape} extended with annotations for LDP container properties
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
	 * @return the input {@code shape} extended with annotations for LDP container properties and pruned of {@linkplain
	 * LDP#CONTAINS ldp:contains} fields, if at least one is actually included; an empty shape, otherwise
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
	 * {@code shape}, if at least one is actually included; the source {@code shape}, otherwise; extended with
	 * annotations for LDP container properties in either cases
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


	//// Mergers ///////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an anchored resource shape.
	 *
	 * @param resource the  resource the shape must be anchored to
	 * @param shape    the shape to be anchored to {@code resource}
	 *
	 * @return a shape extending the input {@code shape} with a filtering-only constraint stating that the focus is
	 * expected to include the anchoring {@code resource}
	 *
	 * @throws NullPointerException if either {@code resource} or {@code shape} is null
	 */
	public static Shape resource(final Resource resource, final Shape shape) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return and(shape, filter().then(all(resource)));
	}

	/**
	 * Creates an anchored container shape.
	 *
	 * @param container the  container the shape must be anchored to
	 * @param shape     the shape to be anchored to {@code container}
	 *
	 * @return a shape extending the input {@code shape} with filtering-only constraints connecting focus resources to
	 * the anchoring {@code container} as required by the LDP container profile identified using {@linkplain Meta
	 * metadata} annotations
	 *
	 * @throws NullPointerException if either {@code container} or {@code shape} is null
	 */
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
					container -> field(inverse((IRI)direct), target(target, container))
					: container -> and();

		} else if ( inverse != null ) {

			return inverse instanceof IRI && target instanceof Resource
					? container -> field((IRI)inverse, target(target, container))
					: container -> and();

		} else {

			return target instanceof Resource
					? container -> field(inverse(LDP.MEMBER), target(target, container))
					: container -> and();

		}
	}


	private static Value target(final Value target, final Resource container) {
		return target.equals(LDP.CONTAINER) ? container
				: target.equals(LDP.RESOURCE) ? container instanceof IRI ? resource((IRI)container) : container
				: target;
	}


	private static IRI resource(final IRI container) {
		return iri(ResourcePattern.matcher(container.stringValue()).replaceAll("${resource}"));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shapes() {} // utility


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ContainerTraverser extends Traverser<Shape> {

		@Override public Shape probe(final Shape shape) {
			return shape;
		}


		@Override public Shape probe(final Field field) {
			return field.getName().equals(LDP.CONTAINS) ? and() : field;
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
			return field.getName().equals(LDP.CONTAINS) ? field.getShape() : and();
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
