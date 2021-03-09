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
import org.eclipse.rdf4j.rio.turtle.TurtleParser;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.metreeca.json.Values.iri;

import static org.assertj.core.api.Assertions.entry;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;


public final class ValuesTest {

	static { // logging not configured: reset and enable fine console logging

		if ( System.getProperty("java.util.logging.config.file") == null
				&& System.getProperty("java.util.logging.config.class") == null ) {

			Logger.getLogger("").setLevel(Level.FINE);

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final String Base="http://example.com/";
	public static final String Namespace=Base+"terms#";


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

	public static Model birt() {
		return dataset(ValuesTest.class.getResource(ValuesTest.class.getSimpleName()+".ttl"));
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


	//// Turtle Codecs /////////////////////////////////////////////////////////////////////////////////////////////////

	public static Model decode(final String turtle) {

		if ( turtle == null ) {
			throw new NullPointerException("null turtle");
		}

		try {

			final StatementCollector collector=new StatementCollector();

			final RDFParser parser=new TurtleParser();

			parser.setPreserveBNodeIDs(true);
			parser.setRDFHandler(collector);

			parser.parse(new StringReader(Prefixes.entrySet().stream()
					.map(entry -> format("@prefix %s: <%s> .", entry.getKey(), entry.getValue()))
					.collect(joining("\n",
							format("@base <%s> .\n\n", Base),
							format("%s\n\n", turtle)
					))
			));


			return new LinkedHashModel(collector.getStatements());

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	public static String encode(final Iterable<Statement> model) {

		final StringWriter writer=new StringWriter();

		Rio.write(model, writer, RDFFormat.TURTLE);

		return writer.toString();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ValuesTest() {} // utility

}
