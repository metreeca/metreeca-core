/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.form.things;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.*;

import static com.metreeca.form.Shape.*;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.MaxLength.maxLength;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.MinInclusive.minInclusive;
import static com.metreeca.form.shapes.Pattern.pattern;
import static com.metreeca.form.things.Values.*;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.stream.Collectors.joining;


public final class ValuesTest {

	static { // logging not configured: reset and enable fine console logging

		if ( System.getProperty("java.util.logging.config.file") == null
				&& System.getProperty("java.util.logging.config.class") == null ) {

			final Level level=Level.FINE;

			LogManager.getLogManager().reset();

			final ConsoleHandler handler=new ConsoleHandler();

			handler.setLevel(level);

			final Logger logger=Logger.getLogger("");

			logger.setLevel(level);
			logger.addHandler(handler);

		}

	}


	private static final Logger logger=Logger.getLogger(ValuesTest.class.getName());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final String Base="http://example.com/";
	public static final String Namespace=Base+"terms#";

	public static final IRI Manager=term("roles/manager");
	public static final IRI Salesman=term("roles/salesman");

	public static final Shape Textual=and(required(), datatype(XMLSchema.STRING));

	public static final Shape Employee=role(Manager, Salesman).then(

			clazz(term("Employee")), // implies rdf:type :Employee

			convey().then(
					server().then(
							field(RDF.TYPE, and(required(), datatype(Form.IRIType))),
							field(RDFS.LABEL, Textual),
							field(term("code"), and(required(), datatype(XMLSchema.STRING), pattern("\\d+")))
					),
					and(
							field(term("forename"), and(required(), datatype(XMLSchema.STRING), maxLength(80))),
							field(term("surname"), and(required(), datatype(XMLSchema.STRING), maxLength(80))),
							field(term("email"), and(required(), datatype(XMLSchema.STRING), maxLength(80))),
							field(term("title"), and(required(), datatype(XMLSchema.STRING), maxLength(80)))
					),
					role(Manager).then(

							field(term("seniority"), and(required(), datatype(XMLSchema.INTEGER),
									minInclusive(literal(integer(1))), maxInclusive(literal(integer(5))))),

							field(term("supervisor"), and(
									optional(), datatype(Form.IRIType), clazz(term("Employee")),
									relate().then(field(RDFS.LABEL, Textual))
							)),
							field(term("subordinate"), and(
									optional(), datatype(Form.IRIType), clazz(term("Employee")),
									relate().then(field(RDFS.LABEL, Textual))
							))

					))

	);

	public static final Shape Employees=role(Manager, Salesman).then(
			hidden().then(
					meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
					meta(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
					meta(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
			),
			convey().then(
					field(RDFS.LABEL, Textual),
					field(RDFS.COMMENT, Textual),
					field(LDP.CONTAINS, and(multiple(), Employee))
			)
	);


	private static final Map<String, String> Prefixes=new LinkedHashMap<String, String>() {{
		put("", Namespace);
		put("birt", Namespace);
		put("rdf", RDF.NAMESPACE);
		put("rdfs", RDFS.NAMESPACE);
		put("xsd", XMLSchema.NAMESPACE);
		put("ldp", LDP.NAMESPACE);
		put("skos", SKOS.NAMESPACE);
		put("form", Form.Namespace);
	}};

	private static final String SPARQLPrefixes=Prefixes.entrySet().stream()
			.map(entry -> "prefix "+entry.getKey()+": <"+entry.getValue()+">")
			.collect(joining("\n"));

	private static final String TurtlePrefixes=Prefixes.entrySet().stream()
			.map(entry -> "@prefix "+entry.getKey()+": <"+entry.getValue()+">.")
			.collect(joining("\n"));


	private static final Map<String, Model> DatasetCache=new HashMap<>();


	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

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


	//// Datasets //////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Model small() {
		return dataset(ValuesTest.class.getResource("ValuesTestSmall.ttl"));
	}

	public static Model large() {
		return dataset(ValuesTest.class.getResource("ValuesTestLarge.ttl"));
	}


	public static Model dataset(final URL resource) {
		return dataset(resource, Base);
	}

	public static Model dataset(final URL resource, final String base) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		return DatasetCache.computeIfAbsent(resource.toExternalForm(), key -> {
			try (final InputStream input=resource.openStream()) {
				return Rio.parse(input, base, RDFFormat.TURTLE).unmodifiable();
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}


	//// RDF Codecs ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Model decode(final String rdf) {
		return decode(rdf, Base);
	}

	public static Model decode(final String rdf, final String base) {
		return decode(rdf, RDFFormat.TURTLE, base);
	}

	public static Model decode(final String rdf, final RDFFormat format) {
		return decode(rdf, format, Base);
	}

	public static Model decode(final String rdf, final RDFFormat format, final String base) { // includes default base/prefixes

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
			parser.parse(new StringReader(format.equals(RDFFormat.TURTLE) ? TurtlePrefixes+"\n\n"+rdf : rdf), base);

			return new LinkedHashModel(collector.getStatements());

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	public static String encode(final Iterable<Statement> model) {
		return encode(model, RDFFormat.TURTLE);
	}

	public static String encode(final Iterable<Statement> model, final RDFFormat format) {

		final StringWriter writer=new StringWriter();

		Rio.write(model, writer, format);

		return writer.toString();
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


	@SafeVarargs public static Supplier<RepositoryConnection> sandbox(final Iterable<Statement>... datasets) {

		if ( datasets == null ) {
			throw new NullPointerException("null datasets");
		}

		final Repository repository=new SailRepository(new MemoryStore());

		repository.initialize();

		try (final RepositoryConnection connection=repository.getConnection()) {
			for (final Iterable<Statement> dataset : datasets) {

				if ( dataset == null ) {
					throw new NullPointerException("null dataset");
				}

				connection.add(dataset);
			}
		}

		return repository::getConnection;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testAnnotatedIRIs() {

		assertThat(direct(RDF.NIL)).isTrue();
		assertThat(direct(inverse(RDF.NIL))).isFalse();

		assertThat(inverse(inverse(RDF.NIL))).as("symmetric").isEqualTo(RDF.NIL);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ValuesTest() {} // utility

}
