/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.link.services;

import com.metreeca.link.Index;
import com.metreeca.link.Request;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.metreeca.jeep.rdf.Values.iri;
import static com.metreeca.jeep.rdf.Values.statement;
import static com.metreeca.jeep.rdf.ValuesTest.assertIsomorphic;
import static com.metreeca.jeep.rdf.ValuesTest.parse;
import static com.metreeca.jeep.rdf.ValuesTest.write;
import static com.metreeca.link.HandlerTest.model;
import static com.metreeca.link.HandlerTest.response;
import static com.metreeca.link.HandlerTest.tools;
import static com.metreeca.link.Link.SysAdm;

import static org.junit.Assert.assertTrue;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;


public final class GraphsTest {

	private static final String GraphsPath="/graphs";
	private static final String GraphsTarget="http://example.org"+GraphsPath;

	private static final Set<Statement> First=singleton(statement(RDF.NIL, RDF.VALUE, RDF.FIRST));
	private static final Set<Statement> Rest=singleton(statement(RDF.NIL, RDF.VALUE, RDF.REST));


	@Test public void testGetGraphCatalog() {
		tools(tools -> {

			new Graphs().load(tools);

			model(tools.get(Graph.Tool), First, (Resource)null);
			model(tools.get(Graph.Tool), Rest, RDF.NIL);

			response(tools,

					tools.get(Index.Tool).get(GraphsPath),

					new Request()

							.setRoles(singleton(SysAdm))
							.setMethod(Request.GET)
							.setTarget(GraphsTarget),

					(request, response) -> assertIsomorphic(asList(

							statement(iri(GraphsTarget), RDF.VALUE, RDF.NIL),
							statement(RDF.NIL, RDF.TYPE, VOID.DATASET)

					), parse(response.getText())));
		});
	}


	@Test public void testGetDefaultGraph() {
		tools(tools -> {

			new Graphs().load(tools);

			final Collection<Statement> model=First;

			model(tools.get(Graph.Tool), model, (Resource)null);

			response(tools,

					tools.get(Index.Tool).get(GraphsPath),

					new Request()

							.setRoles(singleton(SysAdm))
							.setMethod(Request.GET)
							.setTarget(GraphsTarget)
							.setQuery("default"),

					(request, response) -> assertIsomorphic(model, parse(response.getText())));
		});
	}

	@Test public void testGetNamedGraph() {
		tools(tools -> {

			new Graphs().load(tools);

			final Collection<Statement> model=First;

			model(tools.get(Graph.Tool), model, RDF.NIL);

			response(tools,

					tools.get(Index.Tool).get(GraphsPath),

					new Request()

							.setRoles(singleton(SysAdm))
							.setMethod(Request.GET)
							.setTarget(GraphsTarget)
							.setQuery("graph="+RDF.NIL),

					(request, response) -> assertIsomorphic(model, parse(response.getText())));
		});
	}


	@Test public void testPutDefaultGraph() {
		tools(tools -> {

			new Graphs().load(tools);

			final Graph graph=tools.get(Graph.Tool);
			final Index index=tools.get(Index.Tool);

			model(graph, First, (Resource)null);

			response(tools,

					index.get(GraphsPath),

					new Request()

							.setRoles(singleton(SysAdm))
							.setMethod(Request.PUT)
							.setTarget(GraphsTarget)
							.setQuery("default")
							.setText(write(Rest)),

					(request, response) -> assertIsomorphic(Rest, model(graph, (Resource)null)));
		});
	}

	@Test public void testPutNamedGraph() {
		tools(tools -> {

			new Graphs().load(tools);

			final Graph graph=tools.get(Graph.Tool);
			final Index index=tools.get(Index.Tool);

			model(graph, First, RDF.NIL);

			response(tools,

					index.get(GraphsPath),

					new Request()

							.setRoles(singleton(SysAdm))
							.setMethod(Request.PUT)
							.setTarget(GraphsTarget)
							.setQuery("graph="+RDF.NIL)
							.setText(write(Rest)),

					(request, response) -> assertIsomorphic(Rest, strip(model(graph, RDF.NIL))));
		});
	}


	@Test public void testPostDefaultGraph() {
		tools(tools -> {

			new Graphs().load(tools);

			final Graph graph=tools.get(Graph.Tool);
			final Index index=tools.get(Index.Tool);

			model(graph, First, (Resource)null);

			response(tools,

					index.get(GraphsPath),

					new Request()

							.setRoles(singleton(SysAdm))
							.setMethod(Request.POST)
							.setTarget(GraphsTarget)
							.setQuery("default")
							.setText(write(Rest)),

					(request, response) -> assertIsomorphic(merge(First, Rest), model(graph, (Resource)null)));
		});
	}

	@Test public void testPostNamedGraph() {
		tools(tools -> {

			new Graphs().load(tools);

			final Graph graph=tools.get(Graph.Tool);
			final Index index=tools.get(Index.Tool);

			model(graph, First, RDF.NIL);

			response(tools,

					index.get(GraphsPath),

					new Request()

							.setRoles(singleton(SysAdm))
							.setMethod(Request.POST)
							.setTarget(GraphsTarget)
							.setQuery("graph="+RDF.NIL)
							.setText(write(Rest)),

					(request, response) -> assertIsomorphic(merge(First, Rest), strip(model(graph, RDF.NIL))));
		});
	}


	@Test public void testDeleteDefaultGraph() {
		tools(tools -> {

			new Graphs().load(tools);

			final Graph graph=tools.get(Graph.Tool);
			final Index index=tools.get(Index.Tool);

			model(graph, First, (Resource)null);

			response(tools,

					index.get(GraphsPath),

					new Request()

							.setRoles(singleton(SysAdm))
							.setMethod(Request.DELETE)
							.setTarget(GraphsTarget)
							.setQuery("default"),

					(request, response) ->
							assertTrue("empty default graph", model(graph, (Resource)null).isEmpty()));
		});
	}


	@Test public void testDeleteNamedGraph() {
		tools(tools -> {

			new Graphs().load(tools);

			final Graph graph=tools.get(Graph.Tool);
			final Index index=tools.get(Index.Tool);

			model(graph, First, RDF.NIL);

			response(tools,

					index.get(GraphsPath),

					new Request()

							.setRoles(singleton(SysAdm))
							.setMethod(Request.DELETE)
							.setTarget(GraphsTarget)
							.setQuery("graph="+RDF.NIL),

					(request, response) ->
							assertTrue("empty default graph", model(graph, RDF.NIL).isEmpty()));
		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SafeVarargs private final Collection<Statement> merge(final Collection<Statement>... models) {

		final Collection<Statement> merged=new HashSet<>();

		for (final Collection<Statement> model : models) {
			merged.addAll(model);
		}

		return merged;
	}

	private Set<Statement> strip(final Collection<Statement> model) {
		return model.stream()
				.map(s -> statement(s.getSubject(), s.getPredicate(), s.getObject()))
				.collect(Collectors.toSet());
	}

}
