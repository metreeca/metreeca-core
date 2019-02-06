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
import com.metreeca.rest.formats.RDFFormat;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.metreeca.form.Shape.constant;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.And.pass;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.rest.Handler.forbidden;
import static com.metreeca.rest.Handler.refused;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.formats.RDFFormat.rdf;

import static java.lang.Boolean.TRUE;
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
 * <li>redacts the request shape according to the provided {@linkplain #Throttler(Value, Value, UnaryOperator)
 * task/view/area parameters} and the request user roles; {@code mode} redaction is left to final shape consumers;</li>
 *
 * <li>enforces shape-based access control according to the redacted request shape;</li>
 *
 * <li>extends the {@linkplain RDFFormat RDF payload} with the statements inferred from the redacted shape;</li>
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
 * <li>extends the {@linkplain RDFFormat RDF payload} with the statements inferred from the redacted shape;</li>
 *
 * <li>trims {@linkplain RDFFormat RDF payload} statements exceeding the allowed envelope of the redacted shape;</li>
 *
 * <li>associates the redacted shape to the response forwarded to the consumer.</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the response doesn't include a non-empty shape:</p>
 *
 * <ul>
 *
 * <li>trims {@linkplain RDFFormat RDF payload} statements exceeding the {@linkplain Throttler#network(IRI, Iterable)
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


	/**
	 * Creates an identity shape operator.
	 *
	 * @return a shape operator returning its input shape
	 */
	public static UnaryOperator<Shape> entity() {
		return merge((container, resource)
				-> TRUE.equals(constant(resource)) ? container
				: and(container, field(LDP.CONTAINS, resource))
		);
	}

	/**
	 * Creates a container shape operator.
	 *
	 * @return a shape operator returning a shape pruned of {@linkplain LDP#CONTAINS ldp:contains} fields, if one is
	 * actually included, or an empty shape, otherwise
	 */
	public static UnaryOperator<Shape> container() {
		return merge((container, resource) -> TRUE.equals(constant(resource)) ? pass() : container);
	}

	/**
	 * Creates a resource shape operator.
	 *
	 * @return a shape operator returning a shape limited to the shapes associated to {@linkplain LDP#CONTAINS
	 * ldp:contains} fields, if one is actually included, or the source shape, otherwise
	 */
	public static UnaryOperator<Shape> resource() {
		return merge((container, resource) -> TRUE.equals(constant(resource)) ? container : resource);
	}


	private static UnaryOperator<Shape> merge(final BinaryOperator<Shape> merger) {
		return shape -> {

			final Shape container=shape
					.map(new ContainerTraverser())
					.map(new Optimizer());

			final Shape resource=shape
					.map(new ResourceTraverser())
					.map(new Optimizer());

			final Stream<Meta> metas=metas(container) // extact existing metadata annotations
					.entrySet().stream()
					.map(entry -> meta(entry.getKey(), entry.getValue()));

			final Stream<Meta> fields=fields(container) // convert container LDP properties to metadata annotations
					.entrySet().stream()
					.filter(entry -> ContainerMetadata.contains(entry.getKey()))
					.map(entry -> entry(entry.getKey(), all(entry.getValue()).orElseGet(Sets::set)))
					.filter(entry -> entry.getValue().size() == 1)
					.map(entry -> meta(entry.getKey(), entry.getValue().iterator().next()));

			final Shape metadata=and(Stream.concat(metas, fields).collect(toList()));

			// container metadata is added both to container and to resource shape to drive engines

			return merger.apply(and(metadata, container), and(metadata, resource)).map(new Optimizer());

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	private final Value task;
	private final Value view;

	private final UnaryOperator<Shape> area;

	private final Map<Shape, Map<Map<IRI, Set<? extends Value>>, Shape>> cache=new IdentityHashMap<>();


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
	public Throttler(final Value task, final Value view, final UnaryOperator<Shape> area) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		if ( view == null ) {
			throw new NullPointerException("null view");
		}

		if ( area == null ) {
			throw new NullPointerException("null area");
		}

		this.task=task;
		this.view=view;
		this.area=area;
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

			if ( TRUE.equals(constant(shape)) ) {

				return handler.handle(request);

			} else {

				// remove annotations and filtering-only constraints for authorization checks

				final Shape general=shape(shape, true, Form.verify, set(Form.any));
				final Shape authorized=shape(shape, true, Form.verify, roles);
				final Shape redacted=shape(shape, false, null, roles);

				return constant(general) != null ? forbidden(request)
						: constant(authorized) != null ? refused(request)
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

			if ( TRUE.equals(constant(shape)) ) {

				return response
						.pipe(rdf(), rdf -> Value(network(focus, rdf)));

			} else {

				final Shape redacted=shape(shape, false, Form.verify, request.roles());

				return response.shape(redacted)
						.pipe(rdf(), rdf -> Value(envelope(focus, redacted, rdf)))
						.pipe(rdf(), rdf -> Value(expand(focus, redacted, rdf)));

			}

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape shape(final Shape shape, final boolean clean, final IRI mode, final Set<Value> roles) {
		return cache
				.computeIfAbsent(shape, _shape -> new HashMap<>())
				.computeIfAbsent(variables(clean, mode, roles), variables -> shape
						.map(area)
						.map(new Redactor(variables))
						.map(clean ? new Cleaner() : new Inspector<Shape>() {
							@Override public Shape probe(final Shape shape) { return shape; }
						})
						.map(new Optimizer())
				);
	}

	private Map<IRI, Set<? extends Value>> variables(final boolean clean, final IRI mode, final Set<Value> roles) {
		return mode == null ? map(
				entry(RDF.NIL, set(literal(clean))),
				entry(Form.task, set(task)),
				entry(Form.view, set(view)),
				entry(Form.role, roles)
		) : map(
				entry(RDF.NIL, set(literal(clean))),
				entry(Form.task, set(task)),
				entry(Form.view, set(view)),
				entry(Form.mode, set(mode)),
				entry(Form.role, roles)
		);
	}


	private <V extends Collection<Statement>> V expand(final IRI focus, final Shape shape, final V model) {

		model.addAll(shape // add implied statements
				.map(new Outliner(focus)) // shape already redacted for verify mode
				.collect(toList())
		);

		return model;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


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

		@Override public Shape probe(final When when) {
			return when(
					when.getTest(),
					when.getPass().map(this),
					when.getFail().map(this)
			);
		}

	}

}
