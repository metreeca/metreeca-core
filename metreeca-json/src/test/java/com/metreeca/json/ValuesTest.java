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

package com.metreeca.json;

import org.assertj.core.data.MapEntry;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;
import java.util.stream.Stream;

import static com.metreeca.json.Shape.optional;
import static com.metreeca.json.Shape.required;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.json.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;
import static com.metreeca.json.shapes.Pattern.pattern;

import static org.assertj.core.api.Assertions.entry;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;


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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final String Base="http://example.com/";
	public static final String Namespace=Base+"terms#";

	public static final IRI Manager=term("roles/manager");
	public static final IRI Salesman=term("roles/salesman");

	public static final Shape Employee=role(Manager, Salesman).then(

			convey().then(

					server().then(
							field(RDF.TYPE).as(required(), datatype(IRIType)),
							field(RDFS.LABEL).as(required(), datatype(XSD.STRING)),
							field(term("code")).as(required(), datatype(XSD.STRING), pattern("\\d+"))
					),

					and(

							field(term("forename")).as(required(), datatype(XSD.STRING), maxLength(80)),
							field(term("surname")).as(required(), datatype(XSD.STRING), maxLength(80)),
							field(term("email")).as(required(), datatype(XSD.STRING), maxLength(80)),
							field(term("title")).as(required(), datatype(XSD.STRING), maxLength(80))
					),

					role(Manager).then(

							field(term("seniority")).as(required(),
									datatype(XSD.INTEGER),
									minInclusive(literal(integer(1))),
									maxInclusive(literal(integer(5)))
							),

							field(term("supervisor")).as(optional(),
									datatype(IRIType), clazz(term("Employee")),
									relate().then(field(RDFS.LABEL).as(required(), datatype(XSD.STRING)))
							),

							field(term("subordinate")).as(optional(),
									datatype(IRIType), clazz(term("Employee")),
									relate().then(field(RDFS.LABEL).as(required(), datatype(XSD.STRING)))
							)

					)

			),

			delete().then(
					field(term("office"))
			)

	);


	public static final Map<String, String> Prefixes=unmodifiableMap(Stream.of(
			entry("app", "app:/terms#"),
			entry("", Namespace),
			entry("birt", Namespace),
			entry("rdf", RDF.NAMESPACE),
			entry("rdfs", RDFS.NAMESPACE),
			entry("xsd", XSD.NAMESPACE),
			entry("ldp", LDP.NAMESPACE),
			entry("skos", SKOS.NAMESPACE)
	).collect(toMap(MapEntry::getKey, MapEntry::getValue)));

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
			try ( final InputStream input=resource.openStream() ) {
				return Rio.parse(input, base, RDFFormat.TURTLE).unmodifiable();
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}


	//// RDF Codecs ////////////////////////////////////////////////////////////////////////////////////////////////////

	private static String turtle(final String rdf) {
		return TurtlePrefixes+"\n\n"+rdf; // !!! avoid prefix clashes
	}


	public static Model decode(final String rdf) {
		return decode(rdf, Base);
	}

	public static Model decode(final String rdf, final String base) {
		return decode(rdf, RDFFormat.TURTLE, base);
	}

	public static Model decode(final String rdf, final RDFFormat format) {
		return decode(rdf, format, Base);
	}

	public static Model decode(final String rdf, final RDFFormat format, final String base) {

		// includes default base/prefixes

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


	public static String encode(final Iterable<Statement> model) {
		return encode(model, RDFFormat.TURTLE);
	}

	public static String encode(final Iterable<Statement> model, final RDFFormat format) {

		final StringWriter writer=new StringWriter();

		Rio.write(model, writer, format);

		return writer.toString();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ValuesTest() {} // utility

}
