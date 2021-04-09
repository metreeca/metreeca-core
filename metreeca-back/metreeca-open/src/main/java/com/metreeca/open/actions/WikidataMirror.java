/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.open.actions;

import com.metreeca.json.Values;
import com.metreeca.rdf4j.actions.*;
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.Fill;
import com.metreeca.rest.services.Logger;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.*;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Stream;

import static com.metreeca.json.Values.*;
import static com.metreeca.open.actions.Wikidata.ITEM;
import static com.metreeca.open.actions.Wikidata.point;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.Xtream.task;
import static com.metreeca.rest.services.Logger.logger;
import static com.metreeca.rest.services.Logger.time;

import static org.eclipse.rdf4j.common.iteration.Iterations.stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public final class WikidataMirror implements Consumer<Stream<String>>, Function<Stream<String>, Stream<Resource>> {

	public static UnaryOperator<IRI> rewriter(final String external, final String internal) {

		if ( external == null ) {
			throw new NullPointerException("null external");
		}

		if ( internal == null ) {
			throw new NullPointerException("null internal");
		}

		return iri -> Optional.of(iri)
				.map(Value::stringValue)
				.filter(s -> s.startsWith(external))
				.map(s -> iri(internal, md5(s)))
				.orElse(iri);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String item="item";

	private boolean historic;

	private IRI[] contexts={};
	private Set<String> languages=singleton("en");

	private UnaryOperator<IRI> rewriter=UnaryOperator.identity();

	private final Graph source=service(Wikidata::Graph);
	private final Graph target=service(graph());

	private final Logger logger=service(logger());


	public WikidataMirror item(final String item) {

		if ( item == null ) {
			throw new NullPointerException("null item");
		}

		this.item=item;

		return this;
	}

	public WikidataMirror historic(final boolean historic) {

		this.historic=historic;

		return this;
	}

	public WikidataMirror contexts(final IRI... contexts) {

		if ( contexts == null || Arrays.stream(contexts).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null contexts");
		}

		this.contexts=contexts.clone();

		return this;
	}


	public WikidataMirror languages(final String... languages) {

		if ( languages == null || Arrays.stream(languages).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null languages");
		}

		this.languages=new HashSet<>(asList(languages));

		return this;
	}

	public WikidataMirror languages(final Collection<String> languages) {

		if ( languages == null || languages.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null languages");
		}

		this.languages=new HashSet<>(languages);

		return this;
	}


	public WikidataMirror rewriter(final IRI internal) {

		if ( internal == null ) {
			throw new NullPointerException("null internal base IRI");
		}

		return rewriter(internal.stringValue());
	}

	public WikidataMirror rewriter(final String internal) {

		if ( internal == null ) {
			throw new NullPointerException("null internal base IRI");
		}

		return rewriter(rewriter(Wikidata.WD, internal));
	}

	public WikidataMirror rewriter(final UnaryOperator<IRI> rewriter) {

		if ( rewriter == null ) {
			throw new NullPointerException("null rewriter");
		}

		this.rewriter=rewriter;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Statement rewrite(final Statement statement) {
		if ( rewriter.equals(UnaryOperator.identity()) ) { return statement; } else {

			final Resource subject=statement.getSubject();
			final IRI predicate=statement.getPredicate();
			final Value object=statement.getObject();

			return statement(
					subject instanceof IRI ? rewrite((IRI)subject) : subject,
					rewrite(predicate),
					object instanceof IRI ? rewrite((IRI)object) : object
			);

		}
	}

	private Resource rewrite(final Resource resource) {
		return resource instanceof IRI ? rewrite((IRI)resource) : resource;
	}

	private IRI rewrite(final IRI iri) {
		return rewriter.equals(UnaryOperator.identity()) ? iri : rewriter.apply(iri);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void accept(final Stream<String> stream) {
		apply(stream);
	}

	@Override public Stream<Resource> apply(final Stream<String> patterns) {

		final Set<Resource> alive=Collections.newSetFromMap(new ConcurrentHashMap<>());

		Xtream.from(patterns)

				.pipe(this::scan)

				.peek(entry -> alive.add(rewrite(entry.getKey()))) // trace alive items

				.pipe(this::test)
				.pipe(this::sync)

				.sink(this::load);

		reap(alive::contains);

		return alive.stream();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Identifies relevant items in wikidata.
	 *
	 * @param patterns a stream of SPARQL pattern identifying relevant items using the {@link #item(String)} variable
	 *
	 * @return a stream of entries mapping relevant wikidata item identifiers to their version literal
	 */
	private Xtream<Entry<IRI, Value>> scan(final Xtream<String> patterns) {
		return patterns

				.filter(pattern -> pattern != null && !pattern.isEmpty())

				.flatMap(historic ? Stream::of : new Fill<>()

						.model("{\n"
								+"\t{pattern}\n"
								+"\n"
								+"} minus {\n"
								+"\n"
								+"\t?{item} wdt:P576 [] # dissolved, abolished or demolished\n"
								+"\n"
								+"}\n"
						)

						.value("item", item)

						.value("pattern")

				)

				.flatMap(new Fill<>()

						.model("select ?{item} ?__version__ {\n"
								+"\n"
								+"\t{pattern}\n"
								+"\n"
								+"\t?{item} schema:version ?__version__.\n"
								+"\n"
								+"}"
						)

						.value("item", item)

						.value("pattern")

				)

				.flatMap(new TupleQuery()
						.graph(source)
				)

				.map(bindings -> new SimpleImmutableEntry<>(
						(IRI)bindings.getValue(item),
						bindings.getValue("__version__")
				));
	}

	/**
	 * Identifies missing or stale items in the local graph.
	 *
	 * @param items a stream of entries mapping wikidata item identifiers to their current version literals
	 *
	 * @return a stream of missing or stale wikidata item identifiers
	 */
	private Xtream<IRI> test(final Xtream<Entry<IRI, Value>> items) {
		return items

				.batch(1_000)

				.flatMap(new Fill<Collection<? extends Entry<IRI, Value>>>()

						.model("prefix schema: <http://schema.org/>\n"+
								"\n"
								+"select distinct ?external {\n"
								+"\n"
								+"\tvalues (?external ?internal ?current) {\n"
								+"\t\t{entries}\n"
								+"\t"
								+"}\n"
								+"\t\n"
								+"\tfilter not exists { \n"
								+"\n"
								+"\t\t?internal schema:version ?version filter (?version >= ?current)\n"
								+"\n"
								+"\t}\n"
								+"\n"
								+"}"
						)

						.value("entries", solutions -> solutions.stream()
								.map(entry -> String.format("(%s %s %s)",
										format(entry.getKey()),
										format(rewrite(entry.getKey())),
										format(entry.getValue())
								))
								.collect(joining("\n\t\t"))
						)

				)

				.flatMap(new TupleQuery()
						.graph(target)
						.dflt(contexts)
				)

				.map(bindings -> (IRI)bindings.getValue("external"));
	}

	/**
	 * Retrieves item updates from wikidata.
	 *
	 * @param items a stream of wikidata item identifiers to be retrieved
	 *
	 * @return a stream of wikidata item updates; each item description is contained in a single update
	 */
	private Xtream<Collection<Statement>> sync(final Xtream<IRI> items) {
		return items

				.batch(1_000)

				.peek(new Consumer<Collection<?>>() {

					private final AtomicInteger count=new AtomicInteger();

					@Override public void accept(final Collection<?> batch) {
						service(logger()).info(WikidataMirror.this, String.format("syncing items <%,d>/<%,d>",
								batch.size(),
								count.addAndGet(batch.size())
						));
					}

				})

				.flatMap(new Fill<Collection<IRI>>()

						.model("construct {\n"
								+"\n"
								+"\t?s a wikibase:Item; ?p ?o.\n"
								+"\t?p rdfs:label ?pl.\n"
								+"\t?o rdfs:label ?ol.\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\tvalues ?s {\n"
								+"\t\t{items}\n"
								+"\t}\n"
								+"\n"
								+"\t?s ?p ?o filter ( !isLiteral(?o) || lang(?o) in ({languages}) )\n\n"
								+"\toptional { [wikibase:directClaim ?p; rdfs:label ?pl] filter ( lang(?pl) in "
								+"({languages}) ) }\n"
								+"\toptional { ?o rdfs:label ?ol filter ( lang(?ol) in ({languages}) ) "
								+"}\n\n"+
								"}\n"
						)

						.value("items", batch -> batch.stream()
								.map(Values::format)
								.collect(joining("\n\t\t"))
						)

						.value("languages", Stream
								.concat(Stream.of(""), languages.stream()) // empty e.g. for version literals
								.map(Values::quote)
								.collect(joining(", "))
						)

				)

				.map(new GraphQuery()
						.graph(source)
						.andThen(updates -> updates.collect(toList()))
				);
	}

	/**
	 * Loads item updates to the local graph.
	 *
	 * @param updates a stream of wikidata item updates
	 */
	private void load(final Xtream<Collection<Statement>> updates) {
		updates.sequential().forEach(update -> target.update(task(connection -> { // inside a single txn

			Xtream.from(update)

					.filter(pattern(null, RDF.TYPE, ITEM))
					.map(Statement::getSubject)
					.distinct()

					.forEach(external -> {

						final Resource internal=rewrite(external);

						// remove existing data // !!! logging

						connection.remove(internal, null, null, contexts);

						// add alias

						if ( !internal.equals(external) ) {
							connection.add(internal, OWL.SAMEAS, external, contexts);
						}

					});

			Xtream.from(update) // upload updates

					.map(this::rewrite)

					.batch(100_000)

					.forEach(new Upload()
							.graph(target)
							.contexts(contexts)
					);

			Xtream.from(update) // upload WGS84 coordinates

					.filter(pattern(null, Wikidata.P625, null))

					.flatMap(statement -> point(statement.getObject().stringValue())

							.map(point -> {

								final Resource resource=rewrite(statement.getSubject());

								return Stream.of(
										statement(resource, WGS84.LAT, literal(point.getKey())),
										statement(resource, WGS84.LONG, literal(point.getValue()))
								);
							})

							.orElseGet(Stream::empty)

					)

					.batch(100_000)

					.forEach(new Upload()
							.graph(target)
							.contexts(contexts)
					);

		})));
	}

	/**
	 * Remove dead items from the local graph.
	 *
	 * @param alive an internal item aliveness test
	 */
	private void reap(final Predicate<? super Resource> alive) {

		final AtomicInteger reaped=new AtomicInteger();

		time(() -> {

			target.update(task(connection -> stream(connection.getStatements(null, RDF.TYPE, ITEM, contexts))

					.map(Statement::getSubject)

					.filter(alive.negate())

					.peek(resource -> reaped.incrementAndGet())

					// !!! remove symmetric concise bounded description?

					.forEach(resource -> {
						connection.remove(resource, null, null, contexts);
						connection.remove(null, null, resource, contexts);
					})

			));

		}).apply(t ->

				logger.info(this, String.format("reaped <%,d> entities in <%,d> ms", reaped.get(), t))

		);

	}

}
