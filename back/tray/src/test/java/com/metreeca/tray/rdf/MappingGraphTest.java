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

package com.metreeca.tray.rdf;

import com.metreeca.jeep.rdf.Cell;
import com.metreeca.jeep.rdf.ValuesTest;
import com.metreeca.spec.Spec;
import com.metreeca.spec.queries.Items;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.metreeca.jeep.rdf.Values.iri;
import static com.metreeca.jeep.rdf.Values.statement;
import static com.metreeca.jeep.rdf.ValuesTest.*;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shifts.Step.step;
import static com.metreeca.tray.Tray.tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;


public final class MappingGraphTest {

	private static final String External="http://localhost/";
	private static final String Internal=ValuesTest.Base;

	private static final IRI Employee1370=external("employees/1370");
	private static final IRI Account=external("terms#account");
	private static final IRI Customer103=external("customers/103");
	private static final IRI Context=external("");


	private static IRI external(final String name) {
		return iri(External, name);
	}

	private static IRI internal(final String name) {
		return iri(Internal, name);
	}


	//// Shape Operations //////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testShapeGetter() {
		mapping(BIRT, graph -> {

			final Cell cell=graph.get(new Items(all(Employee1370), singletonList(step(Account))));

			final Collection<Value> values=cell.forward(Spec.items).forward(Spec.value).values();

			assertTrue("evaluate results", values.contains(Customer103));

		});
	}


	//// SPARQL Operations /////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testBindings() {
		mapping(BIRT, graph -> graph.browse(connection -> {

			final BooleanQuery query=connection.prepareBooleanQuery(QueryLanguage.SPARQL,
					"ask { $this a <terms#Employee> }", External);

			query.setBinding("this", Customer103);

			assertFalse("evaluate results", query.evaluate());

			return null;

		}));
	}

	@Test public void testDataset() {
		mapping(BIRT, graph -> graph.browse(connection -> {

			final BooleanQuery query=connection.prepareBooleanQuery(QueryLanguage.SPARQL,
					"ask { graph <> { <employees/1370> ?p <customers/103> } }", External);

			final SimpleDataset dataset=new SimpleDataset();

			dataset.addDefaultGraph(RDF.NIL); // disable graph union as default dataset
			dataset.addNamedGraph(Context);

			query.setDataset(dataset);

			assertTrue("evaluate results", query.evaluate());

			return null;

		}));
	}


	@Test public void testBooleanQuery() {
		mapping(BIRT, graph -> graph.browse(connection -> {

			final BooleanQuery query=connection.prepareBooleanQuery(QueryLanguage.SPARQL,
					"ask { <employees/1370> <terms#account> <customers/103> }", External);

			assertTrue("evaluate results", query.evaluate());

			return null;

		}));
	}

	@Test public void testTupleQuery() {
		mapping(BIRT, graph -> graph.browse(connection -> {

			final TupleQuery query=connection.prepareTupleQuery(QueryLanguage.SPARQL,
					"select ?p { <employees/1370> ?p <customers/103> }", External);

			final List<BindingSet> expected=singletonList(new ListBindingSet(singletonList("p"), Account));

			final QueryResultCollector collector=new QueryResultCollector();

			query.evaluate(collector);

			assertEquals("evaluate results", expected, list(query.evaluate()));
			assertEquals("evaluate handler", expected, collector.getBindingSets());

			return null;

		}));
	}

	@Test public void testGraphQuery() {
		mapping(BIRT, graph -> graph.browse(connection -> {

			final GraphQuery query=connection.prepareGraphQuery(QueryLanguage.SPARQL,
					"construct where { <employees/1370> ?p <customers/103> }", External);

			final List<Statement> expected=singletonList(statement(Employee1370, Account, Customer103));

			final StatementCollector collector=new StatementCollector();

			query.evaluate(collector);

			assertEquals("evaluate results", expected, list(query.evaluate()));
			assertEquals("evaluate handler", expected, collector.getStatements());

			return null;

		}));
	}

	@Ignore @Test public void testUpdate() {}


	//// RDF Reading ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testSize() {
		mapping(BIRT, graph -> graph.browse(connection -> {

			assertTrue("mapped context IRIs", connection.size(Context) > 0);

			return null;

		}));
	}

	@Test public void testGetContextIDs() {
		mapping(BIRT, graph -> graph.browse(connection -> {

			assertEquals("mapped context IRIs", singleton(Context), set(connection.getContextIDs()));

			return null;

		}));
	}

	@Test public void testHasStatements() {
		mapping(BIRT, graph -> graph.browse(connection -> {

			assertTrue("mapped pattern", connection.hasStatement(
					Employee1370, Account, Customer103,
					true, Context));

			assertTrue("mapped statement", connection.hasStatement(
					statement(Employee1370, Account, Customer103),
					true, Context));

			return null;

		}));
	}

	@Test public void testGetStatements() {
		mapping(BIRT, graph -> graph.browse(connection -> {

			assertEquals("mapped inferred",
					singleton(statement(Employee1370, Account, Customer103, Context)),
					set(connection.getStatements(Employee1370, Account, Customer103, true, Context)));

			assertEquals("mapped default",
					singleton(statement(Employee1370, Account, Customer103, Context)),
					set(connection.getStatements(Employee1370, Account, Customer103, Context)));

			return null;

		}));
	}


	//// RDF Writing ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Ignore @Test public void testAdd() {}

	@Ignore @Test public void testRemove() {}

	@Ignore @Test public void testClear() {}


	//// RDF Im/Exports ////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testImport() {
		repository(Sandbox, repository -> mapping(() -> repository, graph -> graph.browse(mapped -> {

			try {

				mapped.add(new StringReader(write(singleton(
						statement(external("s"), external("p"), external("o"))
				))), External, RDFFormat.TURTLE);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

			try (final RepositoryConnection direct=repository.getConnection()) {
				assertIsomorphic(
						singleton(statement(internal("s"), internal("p"), internal("o"))),
						list(direct.getStatements(null, null, null))
				);
			}

			return null;

		})));
	}

	@Test public void testExport() {
		repository(Sandbox, repository -> mapping(() -> repository, graph -> graph.browse(mapped -> {

			try (final RepositoryConnection direct=repository.getConnection()) {
				direct.add(internal("s"), internal("p"), internal("o"));
			}

			final StatementCollector collector=new StatementCollector();

			mapped.export(collector);

			assertIsomorphic(
					singleton(statement(external("s"), external("p"), external("o"))),
					collector.getStatements()
			);

			return null;

		})));
	}


	//// Namespaces ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testGetNamespaces() {
		mapping(BIRT, graph -> graph.browse(connection -> {

			final Set<Namespace> expected=singleton(new SimpleNamespace("", External+"terms#"));
			final Set<Namespace> actual=set(connection.getNamespaces());

			assertTrue("evaluate results", actual.containsAll(expected));

			return null;

		}));
	}

	@Test public void testGetNamespace() {
		mapping(BIRT, graph -> graph.browse(connection -> {

			assertEquals("evaluate results", External+"terms#", connection.getNamespace(""));

			return null;

		}));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Object mapping(final Supplier<Repository> supplier, final Consumer<Graph> task) { // !!! refactor
		return repository(supplier, repository -> {

			Tray.tray()

					.set(Graph.Tool, tools -> new Graph("Test Graph", IsolationLevels.SERIALIZABLE, () -> repository) {})

					.exec(() -> task.accept(tool(Graph.Tool).map(External, Internal)))

					.clear();

			return null;

		});
	}

}
