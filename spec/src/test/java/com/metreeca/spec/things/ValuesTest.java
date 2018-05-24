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

package com.metreeca.spec.things;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.ComparisonFailure;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static com.metreeca.spec.things.Values.iri;

import static java.util.stream.Collectors.joining;


public final class ValuesTest {

	private static final Logger logger=Logger.getLogger(ValuesTest.class.getName());


	public static final String Base="http://example.com/";
	public static final String Namespace=Base+"terms#";

	private static final Map<String, String> Prefixes=new LinkedHashMap<String, String>() {{
		put("", Namespace);
		put("birt", Namespace);
		put("rdf", RDF.NAMESPACE);
		put("rdfs", RDFS.NAMESPACE);
		put("xsd", XMLSchema.NAMESPACE);
		put("ldp", LDP.NAMESPACE);
		put("skos", SKOS.NAMESPACE);
	}};

	private static final String SPARQLPrefixes=Prefixes.entrySet().stream()
			.map(entry -> "prefix "+entry.getKey()+": <"+entry.getValue()+">")
			.collect(joining("\n"));

	private static final String TurtlePrefixes=Prefixes.entrySet().stream()
			.map(entry -> "@prefix "+entry.getKey()+": <"+entry.getValue()+">.")
			.collect(joining("\n"));


	private static final Map<String, Model> DatasetCache=new HashMap<>();


	private ValuesTest() {} // a utility class


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static IRI term(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return iri(Namespace, name);
	}

	public static IRI item(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return iri(Base, name);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Model small() {
		return graph(ValuesTest.class.getResource("ValuesTestSmall.ttl"));
	}

	public static Model large() {
		return graph(ValuesTest.class.getResource("ValuesTestLarge.ttl"));
	}


	public static Model graph(final Class<?> master) {
		return graph(master, Base);
	}

	public static Model graph(final Class<?> master, final String base) {

		if ( master == null ) {
			throw new NullPointerException("null master");
		}

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		final String name=master.getSimpleName()+".ttl";
		final URL url=master.getResource(name);

		if ( url == null ) {
			throw new MissingResourceException("dataset", master.getName(), name);
		}

		return graph(url, base);
	}


	public static Model graph(final URL resource) {
		return graph(resource, Base);
	}

	public static Model graph(final URL resource, final String base) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		return DatasetCache.computeIfAbsent(resource.toExternalForm(), key -> {
			try {
				return Rio.parse(resource.openStream(), base, RDFFormat.TURTLE).unmodifiable();
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String turtle(final String turtle) {
		return TurtlePrefixes+"\n\n"+turtle;
	}


	public static Model parse(final String rdf) {
		return parse(rdf, Base);
	}

	public static Model parse(final String rdf, final String base) {
		return parse(rdf, base, RDFFormat.TURTLE);
	}

	public static Model parse(final String rdf, final RDFFormat format) {
		return parse(rdf, Base, format);
	}

	public static Model parse(final String rdf, final String base, final RDFFormat format) { // includes default base/prefixes

		if ( rdf == null ) {
			throw new NullPointerException("null rdf");
		}

		try {

			final StatementCollector collector=new StatementCollector();

			final RDFParser parser=RDFParserRegistry.getInstance().get(format)
					.orElseThrow(() -> new UnsupportedOperationException("unsupported format ["+format+"]"))
					.getParser();

			parser.setPreserveBNodeIDs(true);
			parser.setRDFHandler(collector);
			parser.parse(new StringReader(format.equals(RDFFormat.TURTLE) ? turtle(rdf) : rdf), base);

			return new LinkedHashModel(collector.getStatements());

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	public static String write(final Iterable<Statement> model) {
		return write(model, RDFFormat.TURTLE);
	}

	public static String write(final Iterable<Statement> model, final RDFFormat format) {

		final StringWriter writer=new StringWriter();

		Rio.write(model, writer, format);

		return writer.toString();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String sparql(final String sparql) {
		return SPARQLPrefixes+"\n\n"+sparql; // !!! avoid prefix clashes
	}


	public static List<Map<String, Value>> select(final RepositoryConnection connection, final String sparql) {
		return select(connection, RDF.NIL, sparql);
	}

	public static Model construct(final RepositoryConnection connection, final String sparql) {
		return construct(connection, RDF.NIL, sparql);
	}


	public static List<Map<String, Value>> select(final RepositoryConnection connection, final Value focus, final String sparql) {
		try {

			logger.info("evaluating SPARQL query\n\n\t"
					+sparql.replace("\n", "\n\t")+(sparql.endsWith("\n") ? "" : "\n"));

			final List<Map<String, Value>> tuples=new ArrayList<>();

			final TupleQuery query=connection.prepareTupleQuery(QueryLanguage.SPARQL, sparql(sparql), Base);

			query.setBinding("this", focus);
			query.evaluate(new AbstractTupleQueryResultHandler() {
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

	public static Model construct(final RepositoryConnection connection, final Value focus, final String sparql) {
		try {

			logger.info("evaluating SPARQL query\n\n\t"
					+sparql.replace("\n", "\n\t")+(sparql.endsWith("\n") ? "" : "\n"));

			final Model model=new LinkedHashModel();

			final GraphQuery query=connection.prepareGraphQuery(QueryLanguage.SPARQL, sparql(sparql), Base);

			query.setBinding("this", focus);
			query.evaluate(new StatementCollector(model));

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Asserts that expected and actual statement sequences are isomorphic.
	 *
	 * @param expected the expected statement sequence
	 * @param actual   the actual statement sequence
	 */
	public static void assertIsomorphic(
			final Collection<Statement> expected, final Collection<Statement> actual) {
		assertIsomorphic("", expected, actual);
	}

	public static void assertIsomorphic(final String message,
			final Collection<Statement> expected, final Collection<Statement> actual) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		if ( expected == null ) {
			throw new NullPointerException("null expected");
		}

		if ( actual == null ) {
			throw new NullPointerException("null actual");
		}

		if ( !Models.isomorphic(expected, actual) ) {
			throw new ComparisonFailure(
					message+(message.isEmpty() ? "" : ": ")+"not isomorphic",
					write(new TreeModel(expected), RDFFormat.NTRIPLES),
					write(new TreeModel(actual), RDFFormat.NTRIPLES));
		}
	}


	/**
	 * Asserts that the expected statement sequence is a subset of the actual one.
	 *
	 * @param expected the expected statement sequence
	 * @param actual   the actual statement sequence
	 */
	public static void assertSubset(
			final Collection<Statement> expected, final Collection<Statement> actual) {
		assertSubset("", expected, actual);
	}

	public static void assertSubset(final String message,
			final Collection<Statement> expected, final Collection<Statement> actual) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		if ( expected == null ) {
			throw new NullPointerException("null expected");
		}

		if ( actual == null ) {
			throw new NullPointerException("null actual");
		}

		if ( !Models.isSubset(expected, actual) ) {
			throw new ComparisonFailure(
					message+(message.isEmpty() ? "" : ": ")+"not subsumed",
					write(new TreeModel(expected), RDFFormat.NTRIPLES),
					write(new TreeModel(actual), RDFFormat.NTRIPLES));
		}
	}


	//// !!! Legacy API ////////////////////////////////////////////////////////////////////////////////////////////////

	public static final Supplier<Repository> Sandbox=() -> {

		final Repository repository=new SailRepository(new MemoryStore());

		Runtime.getRuntime().addShutdownHook(new Thread(repository::shutDown));

		repository.initialize();

		return repository;

	};

	public static final Supplier<Repository> BIRT=new Supplier<Repository>() {

		private final Repository repository
				=new SailRepository(new NativeStore(new File(System.getProperty("java.io.tmpdir"), "birt-test")));

		@Override public Repository get() {
			synchronized ( repository ) {

				if ( !repository.isInitialized() ) {

					Runtime.getRuntime().addShutdownHook(new Thread(repository::shutDown));

					repository.initialize();

					try (final RepositoryConnection connection=repository.getConnection()) {

						if ( connection.isEmpty() ) {
							connection.add(
									ValuesTest.class.getResource("ValuesTestLarge.ttl"),
									Base, RDFFormat.TURTLE, iri(Base));
						}

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}

				}

				return repository;
			}
		}
	};


	public static List<Map<String, Value>> select(final Supplier<Repository> supplier, final String query) {
		return connection(supplier, connection -> select(connection, query));
	}


	public static Model construct(final Supplier<Repository> supplier, final String query) {
		return connection(supplier, connection -> construct(connection, query));
	}


	public static <R> R repository(final Function<Repository, R> task) {
		return repository(Sandbox, task);
	}

	public static <R> R connection(final Function<RepositoryConnection, R> task) {
		return connection(Sandbox, task);
	}


	public static <R> R repository(final Supplier<Repository> supplier, final Function<Repository, R> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return task.apply(supplier.get());
	}

	public static <R> R connection(final Supplier<Repository> supplier, final Function<RepositoryConnection, R> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return repository(supplier, repository -> {

			try (final RepositoryConnection connection=repository.getConnection()) {
				try {

					connection.begin(IsolationLevels.SERIALIZABLE);

					return task.apply(connection);

				} finally {

					connection.rollback(); // unconditionally restore initial state

				}
			}

		});
	}

}
