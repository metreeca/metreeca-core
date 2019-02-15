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

package com.metreeca.rest.wrappers;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.*;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Sets;
import com.metreeca.rest.*;
import com.metreeca.rest.bodies.RDFBody;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.metreeca.form.probes.Evaluator.empty;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.shapes.Memo.memoizable;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.rest.Handler.forbidden;
import static com.metreeca.rest.Handler.refused;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.bodies.RDFBody.rdf;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;


/**
 * Content throttler.
 *
 * <p>Controls resource access and throttles content visibility according to user {@linkplain Request#roles() roles}
 * and message {@linkplain Message#shape() shapes}.</p>
 *
 * <p>If the request includes a non-empty shape:</p>
 *
 * <ul>
 *
 * <li>redacts the request shape according to the provided task, view and area {@linkplain #Throttler(Value, Value,
 * Function) parameters} and the request user roles; {@code mode} redaction is left to final shape consumers;</li>
 *
 * <li>enforces shape-based access control according to the redacted request shape;</li>
 *
 * <li>extends the {@linkplain RDFBody RDF payload} with the statements inferred from the redacted shape;</li>
 *
 * <li>associates the redacted shape to the request forwarded to the wrapped handler.</li>
 *
 * </ul>
 *
 * <p>If the response includes a non-empty shape:</p>
 *
 * <ul>
 *
 * <li>redacts the response shape according to the provided {@linkplain #Throttler(Value, Value) task/view
 * parameters} and the request user roles, hiding also filtering-only shapes;</li>
 *
 * <li>extends the {@linkplain RDFBody RDF payload} with the statements inferred from the redacted shape;</li>
 *
 * <li>trims {@linkplain RDFBody RDF payload} statements exceeding the allowed envelope of the redacted shape;</li>
 *
 * <li>associates the redacted shape to the response forwarded to the consumer.</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the response doesn't include a non-empty shape:</p>
 *
 * <ul>
 *
 * <li>trims {@linkplain RDFBody RDF payload} statements exceeding the {@linkplain Throttler#network(IRI, Iterable)
 * connectivity network} of the response focus {@linkplain Message#item() item}.</li>
 *
 * </ul>
 */
public final class Throttler implements Wrapper {

	private static final Set<IRI> ContainerMetadata=set(
			RDF.TYPE,
			LDP.MEMBERSHIP_RESOURCE,
			LDP.HAS_MEMBER_RELATION,
			LDP.IS_MEMBER_OF_RELATION,
			LDP.INSERTED_CONTENT_RELATION
	);

	private static final Function<Shape, Shape> anyone=memoizable(s -> s
			.map(new Redactor(Form.role))
			.map(new Optimizer())
	);


	/**
	 * Creates an identity shape operator.
	 *
	 * @return a shape operator returning its input shape
	 */
	public static UnaryOperator<Shape> entity() {
		return shape -> and(metadata(shape), shape).map(new Optimizer());
	}

	/**
	 * Creates a container shape operator.
	 *
	 * @return a shape operator returning a shape pruned of {@linkplain LDP#CONTAINS ldp:contains} fields, if one is
	 * actually included, or an empty shape, otherwise
	 */
	public static UnaryOperator<Shape> container() {
		return merge((container, resource) -> pass(resource) ? pass() : container);
	}

	/**
	 * Creates a resource shape operator.
	 *
	 * @return a shape operator returning a shape limited to the shapes associated to {@linkplain LDP#CONTAINS
	 * ldp:contains} fields, if one is actually included, or the source shape, otherwise
	 */
	public static UnaryOperator<Shape> resource() {
		return merge((container, resource) -> pass(resource) ? container : resource);
	}


	private static UnaryOperator<Shape> merge(final BinaryOperator<Shape> merger) {
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


	private final Function<Shape, Shape> common;
	private final Function<Shape, Shape> convey;


	/**
	 * Creates a throttler
	 *
	 * @param task a IRI identifying the {@linkplain Form#task task} to be performed by the wrapped handler
	 * @param view a IRI identifying the {@linkplain Form#view view} level for the wrapped handler
	 *
	 * @throws NullPointerException if either {@code task} or {@code view} is null
	 */
	public Throttler(final Value task, final Value view) {
		this(task, view, entity());
	}

	/**
	 * Creates a throttler
	 *
	 * @param task a IRI identifying the {@linkplain Form#task task} to be performed by the wrapped handler
	 * @param view a IRI identifying the {@linkplain Form#view view} level for the wrapped handler
	 * @param area an operator extracting a specific area of interest from the shape associated with incoming requests
	 *
	 * @throws NullPointerException if any argument is null
	 */
	public Throttler(final Value task, final Value view, final Function<Shape, Shape> area) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		if ( view == null ) {
			throw new NullPointerException("null view");
		}

		if ( area == null ) {
			throw new NullPointerException("null area");
		}

		this.common=memoizable(shape -> shape
				.map(area)
				.map(new Redactor(Form.task, task))
				.map(new Redactor(Form.view, view))
				.map(new Optimizer())
		);

		this.convey=memoizable(shape -> shape
				.map(common)
				.map(new Redactor(Form.mode, Form.convey))
				.map(new Optimizer())
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return pre().wrap(post()).wrap(handler);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper pre() {
		return handler -> request -> {

			final IRI focus=request.item();
			final Shape shape=request.shape();
			final Set<Value> roles=request.roles();

			if ( pass(shape) ) {

				return handler.handle(request);

			} else {

				// remove annotations and filtering-only constraints for authorization checks

				final Shape general=shape
						.map(convey)
						.map(anyone);

				final Shape authorized=shape
						.map(convey)
						.map(new Redactor(Form.role, roles))
						.map(new Optimizer());

				final Shape redacted=shape
						.map(common)
						.map(new Redactor(Form.role, roles))
						.map(new Optimizer());

				return empty(general) ? forbidden(request)
						: empty(authorized) ? refused(request)
						: handler.handle(request.shape(redacted)
						.pipe(rdf(), rdf -> Value(expand(focus, authorized, rdf)))
				);

			}

		};
	}

	private Wrapper post() {
		return handler -> request -> handler.handle(request).map(response -> {

			final IRI focus=request.item();
			final Shape shape=response.shape();

			if ( pass(shape) ) {

				return response
						.pipe(rdf(), rdf -> Value(network(focus, rdf)));

			} else {

				final Shape redacted=shape
						.map(convey)
						.map(new Redactor(Form.role, request.roles()))
						.map(new Optimizer());

				return response.shape(redacted)
						.pipe(rdf(), rdf -> Value(envelope(focus, redacted, rdf)))
						.pipe(rdf(), rdf -> Value(expand(focus, redacted, rdf)));

			}

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <V extends Collection<Statement>> V expand(final IRI focus, final Shape shape, final V model) {

		model.addAll(shape // add implied statements
				.map(new Outliner(focus)) // shape already redacted for convey mode
				.collect(toList())
		);

		return model;
	}


	/**
	 * Retrieves a reachable network from a statement source.
	 *
	 * @param focus the resource whose reachable network is to be retrieved
	 * @param model the statement source the description is to be retrieved from
	 *
	 * @return the reachable network of {@code focus} retrieved from {@code model}
	 *
	 * @throws NullPointerException if either {@code focus} or {@code model} is null
	 */
	private Model network(final IRI focus, final Iterable<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		final Model network=new LinkedHashModel();

		final Queue<Value> pending=new ArrayDeque<>(singleton(focus));
		final Collection<Value> visited=new HashSet<>();

		while ( !pending.isEmpty() ) {

			final Value value=pending.remove();

			if ( visited.add(value) ) {
				model.forEach(statement -> {
					if ( statement.getSubject().equals(value) ) {

						network.add(statement);
						pending.add(statement.getObject());

					} else if ( statement.getObject().equals(value) ) {

						network.add(statement);
						pending.add(statement.getSubject());

					}

				});
			}

		}

		return network;
	}

	/**
	 * Retrieves a shape envelope from a statement source.
	 *
	 * @param focus the resource whose envelope is to be retrieved
	 * @param shape the shape whose envelope is to be retrieved
	 * @param model the statement source the description is to be retrieved from
	 *
	 * @return the {@code shape} envelope of {@code focus} retrieved from {@code model}
	 *
	 * @throws NullPointerException if any argument is null
	 */
	private Model envelope(final Value focus, final Shape shape, final Iterable<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return shape.map(new Extractor(model, singleton(focus))).collect(toCollection(LinkedHashModel::new));
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
