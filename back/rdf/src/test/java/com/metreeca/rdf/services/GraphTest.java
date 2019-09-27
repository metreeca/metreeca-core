
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

package com.metreeca.rdf.services;


import com.metreeca.rest.Context;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.Values.literal;
import static com.metreeca.rdf.Values.statement;
import static com.metreeca.rdf.ValuesTest.*;
import static com.metreeca.rdf.services.Graph.auto;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Engine.engine;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;


public final class GraphTest {

	public static Graph graph() {
		return new Graph() {{ repository(new SailRepository(new MemoryStore())); }};
	}


	private static final Logger logger=Logger.getLogger(GraphTest.class.getName());

	private static final String SPARQLPrefixes=Prefixes.entrySet().stream()
			.map(entry -> "prefix "+entry.getKey()+": <"+entry.getValue()+">")
			.collect(joining("\n"));

	private final Statement data=statement(RDF.NIL, RDF.VALUE, RDF.FIRST);


	private void exec(final Runnable... tasks) {
		new Context()
				.set(engine(), GraphEngine::new)
				.set(Graph.graph(), GraphTest::graph)
				.exec(tasks)
				.clear();
	}


	@Test void testConfigureRequest() {
		exec(() -> assertThat(Graph.<Request>query(

				sparql("\n"
						+"construct { \n"
						+"\n"
						+"\t?this\n"
						+"\t\t:time $time;\n"
						+"\t\t:stem $stem;\n"
						+"\t\t:name $name;\n"
						+"\t\t:task $task;\n"
						+"\t\t:base $base;\n"
						+"\t\t:item $item;\n"
						+"\t\t:user $user;\n"

						+"\t\t:custom $custom.\n"
						+"\n"
						+"} where {}\n"
				),

				(request, query) -> query.setBinding("custom", literal(123))

				)

						.apply(new Request()

										.method(Request.POST)
										.base(Base)
										.path("/test/request"),

								new LinkedHashModel(singleton(data))
						)
				)

						.as("bindings configured")
						.hasSubset(decode("\n"
								+"<test/request>\n"
								+"\t:stem <test/>;\n"
								+"\t:name 'request';\n"
								+"\t:task 'POST';\n"
								+"\t:base <>;\n"
								+"\t:item <test/request>;\n"
								+"\t:user form:none.\n"
						))

						.as("timestamp configured")
						.hasStatement(item("test/request"), term("time"), null)

						.as("custom settings applied")
						.hasStatement(item("test/request"), term("custom"), literal(123))

						.as("existing statements forwarded")
						.hasSubset(data)

		);
	}

	@Test void testConfigureResponse() {
		exec(() -> assertThat(Graph.<Response>query(

				sparql("\n"
						+"construct { \n"
						+"\n"
						+"\t?this\n"
						+"\t\t:time $time;\n"
						+"\t\t:stem $stem;\n"
						+"\t\t:name $name;\n"
						+"\t\t:task $task;\n"
						+"\t\t:base $base;\n"
						+"\t\t:item $item;\n"
						+"\t\t:user $user;\n"
						+"\t\t:code $code;\n"

						+"\t\t:custom $custom.\n"
						+"\n"
						+"} where {}\n"
				),

				(request, query) -> query.setBinding("custom", literal(123))

				)

						.apply(new Response(new Request()

										.method(Request.POST)
										.base(Base)
										.path("/test/request"))

										.status(Response.OK)
										.header("Location", Base+"test/response"),

								new LinkedHashModel(singleton(data))
						)
				)

						.as("bindings configured")
						.hasSubset(decode("\n"
								+"<test/response>\n"
								+"\t:stem <test/>;\n"
								+"\t:name 'response';\n"
								+"\t:task 'POST';\n"
								+"\t:base <>;\n"
								+"\t:item <test/request>;\n"
								+"\t:user form:none;\n"
								+"\t:code 200.\n"
						))

						.as("timestamp configured")
						.hasStatement(item("test/response"), term("time"), null)

						.as("custom settings applied")
						.hasStatement(item("test/response"), term("custom"), literal(123))

						.as("existing statements forwarded")
						.hasSubset(data)

		);
	}


	@Test void testGenerateAutoIncrementingIds() {
		exec(() -> {

			final BiFunction<Request, Collection<Statement>, String> auto=auto();

			final Request request=new Request().base(Base).path("/target/");

			final String one=auto.apply(request, emptySet());
			final String two=auto.apply(request, emptySet());

			assertThat(one).isNotEqualTo(two);

			final String item=request.item();
			final String stem=item.substring(0, item.lastIndexOf('/')+1);

			assertThat(model())
					.doesNotHaveStatement(iri(stem, one), null, null)
					.doesNotHaveStatement(iri(stem, two), null, null);

		});
	}


	public static Model model(final Resource... contexts) {
		return service(Graph.graph()).exec(connection -> { return export(connection, contexts); });
	}

	public static Model model(final String sparql) {
		return service(Graph.graph()).exec(connection -> { return construct(connection, sparql); });
	}

	public static List<Map<String, Value>> tuples(final String sparql) {
		return service(Graph.graph()).exec(connection -> { return select(connection, sparql); });
	}


	public static Runnable model(final Iterable<Statement> model, final Resource... contexts) {
		return () -> service(Graph.graph()).exec(connection -> { connection.add(model, contexts); });
	}


	//// Graph Operations //////////////////////////////////////////////////////////////////////////////////////////////

	public static String sparql(final String sparql) {
		return SPARQLPrefixes+"\n\n"+sparql; // !!! avoid prefix clashes
	}


	public static List<Map<String, Value>> select(final RepositoryConnection connection, final String sparql) {
		try {

			logger.info("evaluating SPARQL query\n\n\t"
					+sparql.replace("\n", "\n\t")+(sparql.endsWith("\n") ? "" : "\n"));

			final List<Map<String, Value>> tuples=new ArrayList<>();

			connection
					.prepareTupleQuery(QueryLanguage.SPARQL, sparql(sparql), Base)
					.evaluate(new AbstractTupleQueryResultHandler() {
						@Override public void handleSolution(final BindingSet bindings) {

							final Map<String, Value> tuple=new LinkedHashMap<>();

							for (final Binding binding : bindings) {
								tuple.put(binding.getName(), binding.getValue());
							}

							tuples.add(tuple);

						}
					});

			return tuples;

		} catch ( final MalformedQueryException e ) {

			throw new MalformedQueryException(e.getMessage()+"----\n\n\t"+sparql.replace("\n", "\n\t"));

		}
	}

	public static Model construct(final RepositoryConnection connection, final String sparql) {
		try {

			logger.info("evaluating SPARQL query\n\n\t"
					+sparql.replace("\n", "\n\t")+(sparql.endsWith("\n") ? "" : "\n"));

			final Model model=new LinkedHashModel();

			connection
					.prepareGraphQuery(QueryLanguage.SPARQL, sparql(sparql), Base)
					.evaluate(new StatementCollector(model));

			return model;

		} catch ( final MalformedQueryException e ) {

			throw new MalformedQueryException(e.getMessage()+"----\n\n\t"+sparql.replace("\n", "\n\t"));

		}
	}


	public static Model export(final RepositoryConnection connection, final Resource... contexts) {

		final Model model=new TreeModel();

		connection.export(new StatementCollector(model), contexts);

		return model;
	}

}
