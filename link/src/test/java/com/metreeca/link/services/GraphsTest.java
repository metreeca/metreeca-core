/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.link.services;

import com.metreeca.link.Request;
import com.metreeca.spec.Spec;
import com.metreeca.spec.things.ValuesTest;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.metreeca.link.LinkTest.testbed;
import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.spec.things.Values.statement;
import static com.metreeca.spec.things.ValuesTest.assertIsomorphic;
import static com.metreeca.spec.things.ValuesTest.export;
import static com.metreeca.tray.Tray.tool;

import static org.junit.Assert.assertTrue;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;


public final class GraphsTest {

	private static final Set<Statement> First=singleton(statement(RDF.NIL, RDF.VALUE, RDF.FIRST));
	private static final Set<Statement> Rest=singleton(statement(RDF.NIL, RDF.VALUE, RDF.REST));


	@Test public void testGetGraphCatalog() {
		testbed().service(Graphs::new)

				.dataset(First, (Resource)null)
				.dataset(Rest, RDF.NIL)

				.request(request -> request

						.roles(singleton(Spec.root))
						.method(Request.GET)
						.base(ValuesTest.Base)
						.path(Graphs.Path)
						.done())

				.response(response -> assertIsomorphic(asList(

						statement(iri(ValuesTest.Base, Graphs.Path), RDF.VALUE, RDF.NIL),
						statement(RDF.NIL, RDF.TYPE, VOID.DATASET)

				), response.rdf()));
	}


	@Test public void testGetDefaultGraph() {
		testbed().service(Graphs::new)

				.dataset(First, (Resource)null)

				.request(request -> request

						.roles(singleton(Spec.root))
						.method(Request.GET)
						.base(ValuesTest.Base)
						.path(Graphs.Path)
						.query("default")
						.done())

				.response(response -> assertIsomorphic(First, response.rdf()));
	}

	@Test public void testGetNamedGraph() {
		testbed().service(Graphs::new)

				.dataset(First, RDF.NIL)

				.request(request -> request

						.roles(singleton(Spec.root))
						.method(Request.GET)
						.base(ValuesTest.Base)
						.path(Graphs.Path)
						.query("graph="+RDF.NIL)
						.done())

				.response(response -> assertIsomorphic(First, response.rdf()));
	}


	@Test public void testPutDefaultGraph() {
		testbed().service(Graphs::new)

				.dataset(First, (Resource)null)

				.request(request -> request

						.roles(singleton(Spec.root))
						.method(Request.PUT)
						.base(ValuesTest.Base)
						.path(Graphs.Path)
						.query("default")
						.text(ValuesTest.encode(Rest)))

				.response(response -> {
					try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
						assertIsomorphic(Rest, export(connection, (Resource)null));
					}
				});
	}

	@Test public void testPutNamedGraph() {
		testbed().service(Graphs::new)

				.dataset(First, RDF.NIL)

				.request(request -> request

						.roles(singleton(Spec.root))
						.method(Request.PUT)
						.base(ValuesTest.Base)
						.path(Graphs.Path)
						.query("graph="+RDF.NIL)
						.text(ValuesTest.encode(Rest)))

				.response(response -> {
					try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
						assertIsomorphic(Rest, strip(export(connection, RDF.NIL)));
					}
				});
	}


	@Test public void testPostDefaultGraph() {
		testbed().service(Graphs::new)

				.dataset(First, (Resource)null)

				.request(request -> request

						.roles(singleton(Spec.root))
						.method(Request.POST)
						.base(ValuesTest.Base)
						.path(Graphs.Path)
						.query("default")
						.text(ValuesTest.encode(Rest)))

				.response(response -> {
					try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
						assertIsomorphic(merge(First, Rest), export(connection, (Resource)null));
					}
				});
	}

	@Test public void testPostNamedGraph() {
		testbed().service(Graphs::new)

				.dataset(First, RDF.NIL)

				.request(request -> request

						.roles(singleton(Spec.root))
						.method(Request.POST)
						.base(ValuesTest.Base)
						.path(Graphs.Path)
						.query("graph="+RDF.NIL)
						.text(ValuesTest.encode(Rest)))

				.response(response -> {
					try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
						assertIsomorphic(merge(First, Rest), strip(export(connection, RDF.NIL)));
					}
				});
	}


	@Test public void testDeleteDefaultGraph() {
		testbed().service(Graphs::new)

				.dataset(First, (Resource)null)

				.request(request -> request

						.roles(singleton(Spec.root))
						.method(Request.DELETE)
						.base(ValuesTest.Base)
						.path(Graphs.Path)
						.query("default")
						.done())

				.response(response -> {
					try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
						assertTrue("empty default graph", export(connection, (Resource)null).isEmpty());
					}
				});
	}


	@Test public void testDeleteNamedGraph() {
		testbed().service(Graphs::new)

				.dataset(First, RDF.NIL)

				.request(request -> request

						.roles(singleton(Spec.root))
						.method(Request.DELETE)
						.base(ValuesTest.Base)
						.path(Graphs.Path)
						.query("graph="+RDF.NIL)
						.done())

				.response(response -> {
					try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
						assertTrue("empty default graph", export(connection, RDF.NIL).isEmpty());
					}
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
