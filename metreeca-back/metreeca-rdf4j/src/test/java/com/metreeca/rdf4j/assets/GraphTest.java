
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

package com.metreeca.rdf4j.assets;


import com.metreeca.rest.Response;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.literal;
import static com.metreeca.json.Values.statement;
import static com.metreeca.json.ValuesTest.*;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rest.Context.asset;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;


public final class GraphTest {

	private static final Logger logger=Logger.getLogger(GraphTest.class.getName());

	private static final String SPARQLPrefixes=Prefixes.entrySet().stream()
			.map(entry -> "prefix "+entry.getKey()+": <"+entry.getValue()+">")
			.collect(joining("\n"));

	private final Statement data=statement(RDF.NIL, RDF.VALUE, RDF.FIRST);


	public static void exec(final Runnable... tasks) {
		new com.metreeca.rest.Context()
				.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))
				.exec(tasks)
				.clear();
	}


	@Test void testConfigureRequest() {
		exec(() -> assertThat(Graph.<com.metreeca.rest.Request>query(

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

						.apply(new com.metreeca.rest.Request()

										.method(com.metreeca.rest.Request.POST)
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
								+"\t:user rdf:nil.\n"
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
		exec(() -> assertThat(Graph.<com.metreeca.rest.Response>query(

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

						.apply(new com.metreeca.rest.Response(new com.metreeca.rest.Request()

										.method(com.metreeca.rest.Request.POST)
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
								+"\t:user rdf:nil;\n"
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


	public static Model model(final Resource... contexts) {
		return asset(graph()).exec(connection -> { return export(connection, contexts); });
	}

	public static Model model(final String sparql) {
		return asset(graph()).exec(connection -> { return construct(connection, sparql); });
	}

	public static List<Map<String, Value>> tuples(final String sparql) {
		return asset(graph()).exec(connection -> { return select(connection, sparql); });
	}


	public static Runnable model(final Iterable<Statement> model, final Resource... contexts) {
		return () -> asset(graph()).exec(connection -> { connection.add(model, contexts); });
	}

	public static List<Statement> localized(final Collection<Statement> model, final String... tags) {
		return model.stream()

				.flatMap(s -> Optional

						.of(s.getObject())

						.filter(o -> s.getPredicate().equals(RDFS.LABEL) || s.getPredicate().equals(RDFS.COMMENT))

						.filter(Literal.class::isInstance)
						.map(Literal.class::cast)

						.filter(l -> l.getDatatype().equals(XSD.STRING))
						.map(Literal::getLabel)

						.map(l -> Arrays.stream(tags).map(lang -> statement(s.getSubject(), s.getPredicate(),
								lang.isEmpty() ? literal(l) : literal(l, lang))
						))

						.orElseGet(() -> Stream.of(s))
				)

				.collect(toList());
	}


	//// Graph Operations /////////////////////////////////////////////////////////////////////////////////////////////

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
