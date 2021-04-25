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

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;

import java.io.*;
import java.util.Map;
import java.util.logging.*;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.logging.Level.ALL;
import static java.util.logging.Level.FINE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;


public final class ValuesTest {

	public static final Map<String, String> Prefixes=unmodifiableMap(Stream.of(

			Values.NS,
			RDF.NS, RDFS.NS, XSD.NS,
			OWL.NS, SKOS.NS,
			LDP.NS

	).collect(toMap(Namespace::getPrefix, Namespace::getName)));


	private static final Logger logger=Logger.getLogger("com.metreeca"); // retain reference to prevent gc

	static { log(FINE); }


	public static void log(final Level level) { // logging not configured: reset and configure console logging level
		if ( System.getProperty("java.util.logging.config.file") == null
				&& System.getProperty("java.util.logging.config.class") == null ) {

			logger.setLevel(level);

			for (final Handler handler : Logger.getLogger("").getHandlers()) {
				handler.setLevel(ALL); // enable detailed reporting from children loggers
			}

		}
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
							format("@base <%s> .\n\n", Values.Root),
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
