
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

package com.metreeca.rdf4j.services;


import com.metreeca.json.Frame;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.logging.Logger;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.ValuesTest.Prefixes;
import static com.metreeca.json.ValuesTest.decode;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.Xtream.task;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;


public final class GraphTest {

	private static final Logger logger=Logger.getLogger(GraphTest.class.getName());

	private static final String SPARQLPrefixes=Prefixes.entrySet().stream()
			.map(entry -> "prefix "+entry.getKey()+": <"+entry.getValue()+">")
			.collect(joining("\n"));


	private static final IRI StardogDefault=iri("tag:stardog:api:context:default");

	private static final Frame data=frame(RDF.NIL).value(RDF.VALUE, RDF.FIRST);


	public static void exec(final Runnable... tasks) {
		new Toolbox()
				.set(Graph.graph(), () -> new Graph(new SailRepository(new MemoryStore())))
				.exec(tasks)
				.clear();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testConfigureRequest() {
		exec(() -> assertThat(

				Graph

						.<Request>query(

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

						.apply(

								new Request()

										.method(Request.POST)
										.base(Base)
										.path("/test/request"),

								frame(iri(Base, "/test/request"))
										.value(RDF.VALUE, RDF.NIL)

						).model().collect(toList())

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
						.hasSubset(statement(iri(Base, "/test/request"), RDF.VALUE, RDF.NIL))

		);
	}

	@Test void testConfigureResponse() {
		exec(() ->
						assertThat(Graph

								.<Response>query(

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

				.apply(

						new Response(new Request()

								.method(Request.POST)
								.base(Base)
								.path("/test/request"))

								.status(Response.OK)
								.header("Location", Root+"test/response"),

						frame(iri(Base, "/test/request"))
								.value(RDF.VALUE, RDF.NIL)

				).model().collect(toList())
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
				.hasSubset(statement(iri(Base, "/test/request"), RDF.VALUE, RDF.NIL))

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Model model(final Resource... contexts) {
		return service(Graph.graph()).query(connection -> { return export(connection, contexts); });
	}

	public static Model model(final String sparql) {
		return service(Graph.graph()).query(connection -> { return construct(connection, sparql); });
	}


	public static Runnable model(final Iterable<Statement> model, final Resource... contexts) {
		return () -> service(Graph.graph()).update(task(connection -> connection.add(model, contexts)));
	}


	//// Graph Operations /////////////////////////////////////////////////////////////////////////////////////////////

	static Collection<Statement> graph(final String sparql) {
		return model(sparql)

				.stream()

				// ;(stardog) statement from default context explicitly tagged // !!! review dependency

				.map(statement -> StardogDefault.equals(statement.getContext()) ? statement(
						statement.getSubject(),
						statement.getPredicate(),
						statement.getObject()
				) : statement)

				.collect(toList());
	}

	static String sparql(final String sparql) {
		return SPARQLPrefixes+"\n\n"+sparql; // !!! avoid prefix clashes
	}


	static Model construct(final RepositoryConnection connection, final String sparql) {
		try {

			logger.info("evaluating SPARQL query\n\n\t"
					+indent(sparql)+(sparql.endsWith("\n") ? "" : "\n"));

			final Model model=new LinkedHashModel();

			connection
					.prepareGraphQuery(QueryLanguage.SPARQL, sparql(sparql), Base)
					.evaluate(new StatementCollector(model));

			return model;

		} catch ( final MalformedQueryException e ) {

			throw new MalformedQueryException(e.getMessage()+"----\n\n\t"+indent(sparql));

		}
	}

	public static Model export(final RepositoryConnection connection, final Resource... contexts) {

		final Model model=new TreeModel();

		connection.export(new StatementCollector(model), contexts);

		return model;
	}

}
