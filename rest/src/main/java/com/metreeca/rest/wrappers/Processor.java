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


import com.metreeca.form.Shape;
import com.metreeca.form.probes.Traverser;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.Shape.pass;
import static com.metreeca.form.things.Values.*;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.tray.Tray.tool;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * RDF processor.
 *
 * <p>Process and trims to shape {@linkplain RDFFormat RDF}payloads for incoming request and outgoing responses and
 * executes SPARQL Update post-processing scripts.</p>
 *
 * <p>If the incoming request is not {@linkplain Request#safe() safe}, wrapped handlers are executed inside a single
 * transaction on the system {@linkplain Graph#Factory graph database}, which is automatically committed on {@linkplain
 * Response#success() successful} response or rolled back otherwise.</p>
 */
public final class Processor implements Wrapper {

	/**
	 * Creates a message processing filter for inserting static RDF content.
	 *
	 * @param rdf the RDF content to be inserted in the processed message; parsed using as base the {@linkplain
	 *            Message#item() focus item} of the processed message
	 * @param <T> the type of the processed message
	 *
	 * @return the new message processing filter
	 *
	 * @throws NullPointerException if {@code rdf} is null
	 * @throws RDFParseException    if {@code rdf} is malformed
	 */
	public static <T extends Message<T>> BiFunction<T, Model, Model> rdf(final String rdf) throws RDFParseException {

		if ( rdf == null ) {
			throw new NullPointerException("null rdf");
		}

		final IRI placeholder=iri("placeholder:/");
		final StatementCollector collector=new StatementCollector();

		final RDFParser parser=new TurtleParser();

		parser.setRDFHandler(collector);

		try (final StringReader reader=new StringReader(rdf)) {
			parser.parse(reader, placeholder.stringValue());
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}

		return (response, statements) -> {

			statements.addAll(rewrite(collector.getStatements(), placeholder, response.item()));

			return statements;

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private BiFunction<Request, Model, Model> pre;
	private BiFunction<Response, Model, Model> post;

	private final Collection<String> scripts=new ArrayList<>();

	private final Graph graph=tool(Graph.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Inserts a request RDF pre-processing filter.
	 *
	 * <p>The filter is chained after previously inserted pre-processing filters and executed on incoming requests and
	 * their {@linkplain RDFFormat RDF} payload, if one is present, or ignored, otherwise.</p>
	 *
	 * <p>If the request includes a {@linkplain Message#shape() shape}, the filtered model is trimmed to remove
	 * statements outside the allowed shape envelope.</p>
	 *
	 * @param filter the request RDF pre-processing filter to be inserted; takes as argument an incoming request and its
	 *               {@linkplain RDFFormat RDF} payload and must return a non null filtered RDF model
	 *
	 * @return this processor
	 *
	 * @throws NullPointerException if {@code filter} is null
	 */
	public Processor pre(final BiFunction<Request, Model, Model> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		this.pre=chain(pre, filter);

		return this;
	}

	/**
	 * Inserts a response RDF post-processing filter.
	 *
	 * <p>The filter is chained after previously inserted post-processing filters and executed on {@linkplain
	 * Response#success() successful} outgoing responses and their {@linkplain RDFFormat RDF} payload, if one is
	 * present, or ignored, otherwise.</p>
	 *
	 * <p>If the response includes a {@linkplain Message#shape() shape}, the filtered model is trimmed to remove
	 * statements outside the allowed shape envelope.</p>
	 *
	 * @param filter the response RDF post-processing filter to be inserted; takes as argument a successful outgoing
	 *               response and its {@linkplain RDFFormat RDF} payload and must return a non null filtered RDF model
	 *
	 * @return this processor
	 *
	 * @throws NullPointerException if {@code filter} is null
	 */
	public Processor post(final BiFunction<Response, Model, Model> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		this.post=chain(post, filter);

		return this;
	}

	/**
	 * Inserts a SPARQL Update housekeeping script.
	 *
	 * <p>The script is executed on the shared {@linkplain Graph#Factory graph} tool on {@linkplain Response#success()
	 * successful} request processing by wrapped handlers and before applying {@linkplain #post(BiFunction)
	 * post-processing filters}, with the following pre-defined bindings:</p>
	 *
	 * <table summary="pre-defined bindings">
	 *
	 * <thead>
	 *
	 * <tr>
	 * <th>variable</th>
	 * <th>value</th>
	 * </tr>
	 *
	 * </thead>
	 *
	 * <tbody>
	 *
	 * <tr>
	 * <td>this</td>
	 * <td>the value of the response {@linkplain Response#item() focus item}</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>stem</td>
	 * <td>the {@linkplain IRI#getNamespace() namespace} of the IRI bound to the {@code this} variable</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>name</td>
	 * <td>the local {@linkplain IRI#getLocalName() name} of the IRI bound to the {@code this} variable</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>user</td>
	 * <td>the IRI identifying the {@linkplain Request#user() user} submitting the request</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>time</td>
	 * <td>an {@code xsd:dateTime} literal representing the current system time with millisecond precision</td>
	 * </tr>
	 *
	 * </tbody>
	 *
	 * </table>
	 *
	 * @param script the SPARQL Update housekeeping script to be executed by this processor on successful request
	 *               processing; empty scripts are ignored
	 *
	 * @return this processor
	 *
	 * @throws NullPointerException if {@code script} is null
	 */
	public Processor sync(final String script) {

		if ( script == null ) {
			throw new NullPointerException("null script script");
		}

		if ( !script.isEmpty() ) {
			scripts.add(script);
		}

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return new Connector()
				.wrap(pre())
				.wrap(post())
				.wrap(sync())
				.wrap(handler);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper pre() {
		return handler -> request -> handler.handle(process(request, pre));
	}

	private Wrapper post() {
		return handler -> request -> handler.handle(request)
				.map(response -> response.success() ? process(response, post) : response);
	}

	private Wrapper sync() {
		return handler -> request -> handler.handle(request).map(response -> {

			if ( response.success() && !scripts.isEmpty() ) {
				graph.update(connection -> {

					final IRI item=response.item();
					final IRI stem=iri(item.getNamespace());
					final Literal name=literal(item.getLocalName());

					final IRI user=response.request().user();
					final Literal time=time(true);

					for (final String update : scripts) {

						final Update operation=connection.prepareUpdate(QueryLanguage.SPARQL, update, request.base());

						operation.setBinding("this", item);
						operation.setBinding("stem", stem);
						operation.setBinding("name", name);

						operation.setBinding("user", user);
						operation.setBinding("time", time);

						operation.execute();

					}

				});
			}

			return response;

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <T extends Message<T>> BiFunction<T, Model, Model> chain(
			final BiFunction<T, Model, Model> pipeline, final BiFunction<T, Model, Model> filter
	) {

		final BiFunction<T, Model, Model> checked=(request, model) ->
				requireNonNull(filter.apply(request, model), "null filter return value");

		return (pipeline == null) ? checked
				: (request, model) -> checked.apply(request, pipeline.apply(request, model));
	}

	private <T extends Message<T>> T process(final T message, final BiFunction<T, Model, Model> filter) {

		// ;( memoize current message state, before it's possibly altered by downstream wrappers

		final IRI focus=message.item();
		final Shape shape=message.shape();

		return message.pipe(RDFFormat.rdf(), statements -> Value((filter == null) ?
				statements : trim(focus, shape, filter.apply(message, new LinkedHashModel(statements)))
		));
	}

	private <T extends Message<T>> Collection<Statement> trim(final Value focus, final Shape shape, final Model model) {
		return pass(shape) ? model : shape // !!! trim to reachable cell / migrate wildcard handling to Trimmer?
				.map(new Trimmer(model, singleton(focus)))
				.collect(toList());
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Model trimmer.
	 *
	 * <p>Recursively extracts all the statements compatible with a shape from a model and an initial collection of
	 * source values .</p>
	 */
	private static final class Trimmer extends Traverser<Stream<Statement>> {

		private final Collection<Statement> model;
		private final Collection<Value> focus;


		private Trimmer(final Collection<Statement> model, final Collection<Value> focus) {
			this.model=model;
			this.focus=focus;
		}


		@Override public Stream<Statement> probe(final Shape shape) {
			return Stream.empty();
		}


		@Override public Stream<Statement> probe(final Trait trait) {

			final Step step=trait.getStep();

			final IRI iri=step.getIRI();
			final boolean inverse=step.isInverse();

			final Function<Statement, Value> source=inverse
					? Statement::getObject
					: Statement::getSubject;

			final Function<Statement, Value> target=inverse
					? Statement::getSubject
					: Statement::getObject;

			final Collection<Statement> restricted=model.stream()
					.filter(s -> focus.contains(source.apply(s)) && iri.equals(s.getPredicate()))
					.collect(toList());

			final Set<Value> focus=restricted.stream()
					.map(target)
					.collect(toSet());

			return Stream.concat(restricted.stream(), trait.getShape().map(new Trimmer(model, focus)));
		}

		@Override public Stream<Statement> probe(final Virtual virtual) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}


		@Override public Stream<Statement> probe(final And and) {
			return and.getShapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Statement> probe(final Or or) {
			return or.getShapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Statement> probe(final Option option) {
			return Stream.concat(option.getPass().map(this), option.getFail().map(this));
		}

	}

}
