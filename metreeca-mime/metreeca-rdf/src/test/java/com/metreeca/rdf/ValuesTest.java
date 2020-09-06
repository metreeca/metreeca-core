/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf;

import com.metreeca.json.Shape;

import org.assertj.core.data.MapEntry;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;
import java.util.stream.Stream;

import static com.metreeca.json.Shape.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.Meta.meta;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;
import static com.metreeca.json.shapes.Pattern.pattern;
import static com.metreeca.rdf.Values.*;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;


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

	public static final Shape Textual=and(required(), datatype(XSD.STRING));

	public static final Shape Employee=role(Manager, Salesman).then(

			convey().then(

					server().then(
							field(RDF.TYPE, and(required(), datatype(IRIType))),
							field(RDFS.LABEL, Textual),
							field(term("code"), and(required(), datatype(XSD.STRING), pattern("\\d+")))
					),

					and(

							field(term("forename"), and(required(), datatype(XSD.STRING), maxLength(80))),
							field(term("surname"), and(required(), datatype(XSD.STRING), maxLength(80))),
							field(term("email"), and(required(), datatype(XSD.STRING), maxLength(80))),
							field(term("title"), and(required(), datatype(XSD.STRING), maxLength(80)))
					),

					role(Manager).then(

							field(term("seniority"), and(required(), datatype(XSD.INTEGER),
									minInclusive(literal(integer(1))), maxInclusive(literal(integer(5))))),

							field(term("supervisor"), and(
									optional(), datatype(IRIType), clazz(term("Employee")),
									relate().then(field(RDFS.LABEL, Textual))
							)),

							field(term("subordinate"), and(
									optional(), datatype(IRIType), clazz(term("Employee")),
									relate().then(field(RDFS.LABEL, Textual))
							))

					)

			),

			delete().then(
					field(term("office"))
			)

	);

	public static final Shape Employees=role(Manager, Salesman).then(
			meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
			meta(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
			meta(LDP.MEMBERSHIP_RESOURCE, term("Employee")),
			convey().then(
					field(RDFS.LABEL, Textual),
					field(RDFS.COMMENT, Textual),
					field(LDP.CONTAINS, and(multiple(), Employee))
			)
	);


	public static final Map<String, String> Prefixes=unmodifiableMap(Stream.of(
			entry("", Namespace),
			entry("app", Internal),
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

	public static String turtle(final String rdf) {
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

	public static Model decode(final String rdf, final RDFFormat format, final String base) { // includes default
		// base/prefixes

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

	@Test void testAnnotatedIRIs() {

		assertThat(direct(RDF.NIL)).isTrue();
		assertThat(direct(inverse(RDF.NIL))).isFalse();

		assertThat(inverse(inverse(RDF.NIL))).as("symmetric").isEqualTo(RDF.NIL);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ValuesTest() {} // utility

}
