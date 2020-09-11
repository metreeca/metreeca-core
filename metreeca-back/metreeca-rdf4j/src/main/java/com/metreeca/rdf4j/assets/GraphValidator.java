/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf4j.assets;

import com.metreeca.json.*;
import com.metreeca.json.shapes.*;
import com.metreeca.rest.Either;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.json.Shape.shape;
import static com.metreeca.json.Trace.trace;
import static com.metreeca.json.Values.*;
import static com.metreeca.rdf.formats.JSONLDFormat.jsonld;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rdf4j.assets.Snippets.source;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.UnprocessableEntity;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.eclipse.rdf4j.common.iteration.Iterations.stream;


final class GraphValidator extends GraphProcessor {

	private final Graph graph=asset(graph());


	<M extends com.metreeca.rest.Message<M>> com.metreeca.rest.Either<com.metreeca.rest.MessageException, M> validate(final M message) {
		return message

				.body(jsonld())

				.flatMap(rdf -> validate(iri(message.item()), convey(message.attribute(shape())), rdf).fold(
						trace -> Left(status(UnprocessableEntity, trace.toJSON())), model -> Right(message)
				));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Either<Trace, Collection<Statement>> validate(
			final IRI resource, final Shape shape, final Collection<Statement> model) {
		return graph.exec(connection -> {

			final Collection<Statement> envelope=new HashSet<>();

			final Trace trace=shape.map(new ValidatorProbe(
					connection, resource, singleton(resource), model, envelope
			));

			final Map<String, Collection<Object>> issues=model.stream()

					.filter(statement -> !envelope.contains(statement))

					.collect(toMap(

							statement -> statement.getSubject().equals(resource) ?
									"unexpected property {"+format(statement.getPredicate())+"}"
									: statement.getObject().equals(resource) ?
									"unexpected property {^"+format(statement.getPredicate())+"}"
									: "statement outside shape envelope",

							Collections::singleton,

							(x, y) -> Stream.of(x, y).flatMap(Collection::stream).collect(toSet())

					));


			final Trace merged=trace(trace(issues), trace);

			return merged.empty() ? Right(model) : Left(merged);

		});
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final class ValidatorProbe implements Shape.Probe<Trace> {

		private final RepositoryConnection connection;

		private final IRI resource;
		private final Collection<Value> focus;

		private final Collection<Statement> source;
		private final Collection<Statement> target;


		private ValidatorProbe(
				final RepositoryConnection connection,
				final IRI resource, final Collection<Value> focus,
				final Collection<Statement> source, final Collection<Statement> target
		) {

			this.connection=connection;

			this.resource=resource;
			this.focus=focus;

			this.source=source;
			this.target=target;

		}


		private Value value(final Value value) {
			return value instanceof Focus
					? ((Focus)value).resolve(resource)
					: value;
		}

		private Set<Value> values(final Collection<Value> values) {
			return values.stream()
					.map(this::value)
					.collect(toSet());
		}


		private <T> Predicate<T> negate(final Predicate<T> predicate) {
			return predicate.negate();
		}

		private String issue(final Shape shape) {
			return shape.toString().replaceAll("\\s+", " ");
		}


		@Override public Trace probe(final Meta meta) {
			return trace();
		}

		@Override public Trace probe(final Guard guard) {
			throw new UnsupportedOperationException(guard.toString());
		}


		@Override public Trace probe(final Datatype datatype) {

			final IRI iri=datatype.id();

			return trace(focus.stream()
					.filter(negate(value -> is(value, iri)))
					.collect(toMap(v -> issue(datatype), Collections::singleton))
			);
		}

		@Override public Trace probe(final Clazz clazz) {
			if ( focus.isEmpty() ) { return trace(); } else {

				// retrieve the class hierarchy rooted in the expected class

				final Set<Value> hierarchy=stream(connection.prepareTupleQuery(source(

						"# clazz hierarchy\n"
								+"\n"
								+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
								+"\n"
								+"select distinct ?class { ?class rdfs:subClassOf* {root} }",

						format(clazz.id())

				)).evaluate())

						.map(bindings -> bindings.getValue("class"))
						.collect(toSet());


				// retrieve type info for focus nodes

				final Map<Value, Set<Value>> types=Stream.concat(

						// retrieve type info from the validated model

						focus.stream().flatMap(value -> source.stream()
								.filter(pattern(value, RDF.TYPE, null))
								.map(Statement::getObject)
								.map(type -> new SimpleImmutableEntry<>(value, type))
						),

						// retrieve type info from graph

						stream(connection.prepareTupleQuery(source(

								"# type info\n"
										+"\n"
										+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
										+"\n"
										+"select ?value ?type {\n"
										+"\n"
										+"\tvalues ?value {\n"
										+"\t\t{values}\n"
										+"\t}\n"
										+"\n"
										+"\t?value a ?type\n"
										+"\n"
										+"}",

								Snippets.list(focus.stream().map(Values::format), "\n")

						)).evaluate()).map(bindings -> new SimpleImmutableEntry<>(
								bindings.getValue("value"),
								bindings.getValue("type")
						))

				).collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toSet())));

				return trace(focus.stream()
						.filter(value -> disjoint(types.getOrDefault(value, emptySet()), hierarchy))
						.collect(toMap(v -> issue(clazz), Collections::singleton))
				);

			}
		}


		@Override public Trace probe(final MinExclusive minExclusive) {

			final Value limit=value(minExclusive.value());

			return trace(focus.stream()
					.filter(negate(value -> comparator.compare(value, limit) > 0))
					.collect(toMap(v -> issue(minExclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxExclusive maxExclusive) {

			final Value limit=value(maxExclusive.value());

			return trace(focus.stream()
					.filter(negate(value -> comparator.compare(value, limit) < 0))
					.collect(toMap(v -> issue(maxExclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MinInclusive minInclusive) {

			final Value limit=value(minInclusive.value());

			return trace(focus.stream()
					.filter(negate(value -> comparator.compare(value, limit) >= 0))
					.collect(toMap(v -> issue(minInclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxInclusive maxInclusive) {

			final Value limit=value(maxInclusive.value());

			return trace(focus.stream()
					.filter(negate(value -> comparator.compare(value, limit) <= 0))
					.collect(toMap(v -> issue(maxInclusive), Collections::singleton))
			);
		}


		@Override public Trace probe(final MinLength minLength) {

			final int limit=minLength.limit();

			return trace(focus.stream()
					.filter(negate(value -> text(value).length() >= limit))
					.collect(toMap(value -> issue(minLength), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxLength maxLength) {

			final int limit=maxLength.limit();

			return trace(focus.stream()
					.filter(negate(value -> text(value).length() <= limit))
					.collect(toMap(value -> issue(maxLength), Collections::singleton))
			);
		}

		@Override public Trace probe(final Pattern pattern) {

			final String expression=pattern.text();
			final String flags=pattern.flags();

			final java.util.regex.Pattern compiled=java.util.regex.Pattern
					.compile(flags.isEmpty() ? expression : "(?"+flags+":"+expression+")");

			// match the whole string: don't use compiled.asPredicate() (implemented using .find())

			return trace(focus.stream()
					.filter(negate(value -> compiled.matcher(text(value)).matches()))
					.collect(toMap(value -> issue(pattern), Collections::singleton))
			);
		}

		@Override public Trace probe(final Like like) {

			final String expression=like.toExpression();

			final Predicate<String> predicate=java.util.regex.Pattern.compile(expression).asPredicate();

			return trace(focus.stream()
					.filter(negate(value -> predicate.test(text(value))))
					.collect(toMap(v -> issue(like), Collections::singleton))
			);
		}


		@Override public Trace probe(final MinCount minCount) {

			final int count=focus.size();
			final int limit=minCount.limit();

			return count >= limit ? trace() : trace(issue(minCount), count);
		}

		@Override public Trace probe(final MaxCount maxCount) {

			final int count=focus.size();
			final int limit=maxCount.limit();

			return count <= limit ? trace() : trace(issue(maxCount), count);
		}

		@Override public Trace probe(final In in) {

			final Set<Value> range=values(in.values());

			final List<Value> unexpected=focus
					.stream()
					.filter(negate(range::contains))
					.collect(toList());

			return unexpected.isEmpty() ? trace() : trace(issue(in), unexpected);
		}

		@Override public Trace probe(final All all) {

			final Set<Value> range=values(all.values());

			final List<Value> missing=range
					.stream()
					.filter(negate(focus::contains))
					.collect(toList());

			return missing.isEmpty() ? trace() : trace(issue(all), missing);
		}

		@Override public Trace probe(final Any any) {
			return !disjoint(focus, values(any.values()))
					? trace() : trace(issue(any));
		}


		@Override public Trace probe(final Field field) {

			final IRI iri=field.name();
			final Shape shape=field.shape();

			return focus.stream().map(value -> { // for each focus value

				// compute the new focus set

				final boolean direct=direct(iri);

				final Set<Statement> edges=(direct

						? source.stream().filter(pattern(value, iri, null))
						: source.stream().filter(pattern(null, inverse(iri), value))

				).collect(toSet());

				final Set<Value> focus=(direct

						? edges.stream().map(Statement::getObject)
						: edges.stream().map(Statement::getSubject)

				).collect(toSet());

				// trace visited statements

				target.addAll(edges);

				// validate the field shape on the new focus set

				return trace(emptyMap(), singletonMap(iri,
						shape.map(new ValidatorProbe(connection, resource, focus, source, target))
				));

			}).reduce(trace(), Trace::trace);
		}


		@Override public Trace probe(final And and) {
			return and.shapes().stream()
					.map(s -> s.map(this))
					.reduce(trace(), Trace::trace);
		}

		@Override public Trace probe(final Or or) {
			return or.shapes().stream().anyMatch(s -> s.map(this).empty()) ? trace() : trace(issue(or));
		}

		@Override public Trace probe(final When when) {
			throw new UnsupportedOperationException(when.toString());
		}

	}

}

