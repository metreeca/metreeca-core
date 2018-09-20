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

package com.metreeca.rest.handlers.sparql;

import com.metreeca.form.Form;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats._RDF;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.assertIsomorphic;
import static com.metreeca.tray.Tray.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static java.util.Collections.singleton;


@Deprecated final class GraphsTest {

	private static final Set<Statement> First=singleton(statement(RDF.NIL, RDF.VALUE, RDF.FIRST));
	private static final Set<Statement> Rest=singleton(statement(RDF.NIL, RDF.VALUE, RDF.REST));


	@Test @Disabled void testGetGraphCatalog() {
		//LinkTest.testbed().service(Graphs::new)
		//
		//		.dataset(First, (Resource)null)
		//		.dataset(Rest, RDF.NIL)
		//
		//		.request(request -> request
		//
		//				.roles(singleton(Form.root))
		//				.method(Request.GET)
		//				.base(ValuesTest.Base)
		//				.path(Graphs.Path)
		//				.done())
		//
		//		.response(response -> ValuesTest.assertIsomorphic(asList(
		//
		//				statement(iri(ValuesTest.Base, Graphs.Path), RDF.VALUE, RDF.NIL),
		//				statement(RDF.NIL, RDF.TYPE, VOID.DATASET)
		//
		//		), response.rdf()));
	}


	@Test void testGetDefaultGraph() {
		new Tray()

				.run(() -> tool(Graph.Factory).update(connection -> { connection.add(First); }))

				.get(Graphs::new)

				.handle(new Request()

						.roles(Form.root)
						.method(Request.GET)
						.base(ValuesTest.Base)
						.query("default"))

				.accept(response -> {

					assertEquals(Response.OK, response.status());
					assertIsomorphic(First, response.body(_RDF.Format).value().orElseGet(() -> fail("no RDF body")));

				});
	}

	@Test @Disabled void testGetNamedGraph() {
		//LinkTest.testbed().service(Graphs::new)
		//
		//		.dataset(First, RDF.NIL)
		//
		//		.request(request -> request
		//
		//				.roles(singleton(Form.root))
		//				.method(Request.GET)
		//				.base(ValuesTest.Base)
		//				.path(Graphs.Path)
		//				.query("graph="+RDF.NIL)
		//				.done())
		//
		//		.response(response -> ValuesTest.assertIsomorphic(First, response.rdf()));
	}


	@Test @Disabled void testPutDefaultGraph() {
		//LinkTest.testbed().service(Graphs::new)
		//
		//		.dataset(First, (Resource)null)
		//
		//		.request(request -> request
		//
		//				.roles(singleton(Form.root))
		//				.method(Request.PUT)
		//				.base(ValuesTest.Base)
		//				.path(Graphs.Path)
		//				.query("default")
		//				.text(ValuesTest.encode(Rest)))
		//
		//		.response(response -> {
		//			try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
		//				ValuesTest.assertIsomorphic(Rest, ValuesTest.export(connection, (Resource)null));
		//			}
		//		});
	}

	@Test @Disabled void testPutNamedGraph() {
		//LinkTest.testbed().service(Graphs::new)
		//
		//		.dataset(First, RDF.NIL)
		//
		//		.request(request -> request
		//
		//				.roles(singleton(Form.root))
		//				.method(Request.PUT)
		//				.base(ValuesTest.Base)
		//				.path(Graphs.Path)
		//				.query("graph="+RDF.NIL)
		//				.text(ValuesTest.encode(Rest)))
		//
		//		.response(response -> {
		//			try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
		//				ValuesTest.assertIsomorphic(Rest, strip(ValuesTest.export(connection, RDF.NIL)));
		//			}
		//		});
	}


	@Test void testPostDefaultGraph() {
		//LinkTest.testbed().service(Graphs::new)
		//
		//		.dataset(First, (Resource)null)
		//
		//		.request(request -> request
		//
		//				.roles(singleton(Form.root))
		//				.method(Request.POST)
		//				.base(ValuesTest.Base)
		//				.path(Graphs.Path)
		//				.query("default")
		//				.text(ValuesTest.encode(Rest)))
		//
		//		.response(response -> {
		//			try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
		//				ValuesTest.assertIsomorphic(merge(First, Rest), ValuesTest.export(connection, (Resource)null));
		//			}
		//		});
	}

	@Test @Disabled void testPostNamedGraph() {
		//LinkTest.testbed().service(Graphs::new)
		//
		//		.dataset(First, RDF.NIL)
		//
		//		.request(request -> request
		//
		//				.roles(singleton(Form.root))
		//				.method(Request.POST)
		//				.base(ValuesTest.Base)
		//				.path(Graphs.Path)
		//				.query("graph="+RDF.NIL)
		//				.text(ValuesTest.encode(Rest)))
		//
		//		.response(response -> {
		//			try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
		//				ValuesTest.assertIsomorphic(merge(First, Rest), strip(ValuesTest.export(connection, RDF.NIL)));
		//			}
		//		});
	}


	@Test @Disabled void testDeleteDefaultGraph() {
		//LinkTest.testbed().service(Graphs::new)
		//
		//		.dataset(First, (Resource)null)
		//
		//		.request(request -> request
		//
		//				.roles(singleton(Form.root))
		//				.method(Request.DELETE)
		//				.base(ValuesTest.Base)
		//				.path(Graphs.Path)
		//				.query("default")
		//				.done())
		//
		//		.response(response -> {
		//			try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
		//				assertTrue("empty default graph", ValuesTest.export(connection, (Resource)null).isEmpty());
		//			}
		//		});
	}


	@Test @Disabled void testDeleteNamedGraph() {
		//LinkTest.testbed().service(Graphs::new)
		//
		//		.dataset(First, RDF.NIL)
		//
		//		.request(request -> request
		//
		//				.roles(singleton(Form.root))
		//				.method(Request.DELETE)
		//				.base(ValuesTest.Base)
		//				.path(Graphs.Path)
		//				.query("graph="+RDF.NIL)
		//				.done())
		//
		//		.response(response -> {
		//			try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
		//				assertTrue("empty default graph", ValuesTest.export(connection, RDF.NIL).isEmpty());
		//			}
		//		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SafeVarargs private final Collection<Statement> merge(final Collection<Statement>... models) {

		final Collection<Statement> merged=new HashSet<>();

		for (final Collection<Statement> model : models) {
			merged.addAll(model);
		}

		return merged;
	}

	private Set<Statement> strip(final Collection<Statement> model) { // strip context info
		return model.stream()
				.map(s -> statement(s.getSubject(), s.getPredicate(), s.getObject()))
				.collect(Collectors.toSet());
	}

}
