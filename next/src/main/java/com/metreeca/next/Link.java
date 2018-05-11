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

package com.metreeca.next;

import org.eclipse.rdf4j.model.IRI;

import java.io.*;
import java.util.Properties;
import java.util.regex.Pattern;

import static com.metreeca.spec.Values.iri;


/**
 * Server configuration RDF vocabulary.
 */
public final class Link {

	/**
	 * Server build number.
	 */
	public static final String Build;

	/**
	 * Server identification token.
	 *
	 * A semantically versioned identification token suitable for inclusion in HTTP {@code Server} headers.
	 *
	 * @see <a href="http://semver.org">Semantic Versioning 2.0.0</a>
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.2">RFC 7231 Hypertext Transfer Protocol (HTTP/1.1):
	 * Semantics and Content § 7.4.2. Server</a>
	 */
	public static final String Token;


	private static final Pattern HTMLPattern=Pattern.compile("\\btext/x?html\\b");

	static {
		try (
				final InputStream input=Link.class.getResourceAsStream("Link.properties");
				final Reader reader=new InputStreamReader(input, "UTF-8");
		) {

			final Properties properties=new Properties();

			properties.load(reader);

			Build=properties.getProperty("timestamp");
			Token="Metreeca/"+properties.getProperty("version")+"+"+Build;

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final String Namespace="tag:com.metreeca,2016:link/terms#"; // keep aligned with client


	//// Port Specs ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI Port=iri(Namespace, "Port"); // port class

	public static final IRI path=iri(Namespace, "path"); // port path
	public static final IRI uuid=iri(Namespace, "uuid"); // port uuid
	public static final IRI spec=iri(Namespace, "spec"); // port spec

	public static final IRI root=iri(Namespace, "root"); // port root resource flag
	public static final IRI soft=iri(Namespace, "soft"); // port soft management flag

	public static final IRI create=iri(Namespace, "create"); // port create SPARQL post-processing hook
	public static final IRI relate=iri(Namespace, "relate"); // port relate SPARQL post-processing hook
	public static final IRI update=iri(Namespace, "update"); // port update SPARQL post-processing hook
	public static final IRI delete=iri(Namespace, "delete"); // port delete SPARQL post-processing hook
	public static final IRI mutate=iri(Namespace, "mutate"); // port mutate SPARQL post-processing hook


	//// Extended LDP Resource Types ///////////////////////////////////////////////////////////////////////////////////

	public static final IRI ShapedContainer=iri(Namespace, "ShapedContainer");
	public static final IRI ShapedResource=iri(Namespace, "ShapedResource");


	//// Server Metadata ///////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI ServerVersion=iri(Namespace, "server-version");

	public static final IRI SystemName=iri(Namespace, "system-name");
	public static final IRI SystemVersion=iri(Namespace, "system-version");
	public static final IRI SystemArchitecture=iri(Namespace, "system-architecture");
	public static final IRI SystemProcessors=iri(Namespace, "system-processors");

	public static final IRI RuntimeSpecName=iri(Namespace, "runtime-spec-name");
	public static final IRI RuntimeSpecVendor=iri(Namespace, "runtime-spec-vendor");
	public static final IRI RuntimeSpecVersion=iri(Namespace, "runtime-spec-version");

	public static final IRI RuntimeVMName=iri(Namespace, "runtime-vm-name");
	public static final IRI RuntimeVMVendor=iri(Namespace, "runtime-vm-vendor");
	public static final IRI RuntimeVMVersion=iri(Namespace, "runtime-vm-version");

	public static final IRI RuntimeMemoryTotal=iri(Namespace, "runtime-memory-total");
	public static final IRI RuntimeMemoryUsage=iri(Namespace, "runtime-memory-usage");

	public static final IRI Backend=iri(Namespace, "backend");

	public static final IRI Setup=iri(Namespace, "setup"); // range: dictionary
	public static final IRI Properties=iri(Namespace, "properties"); // range: dictionary


	//// Dictionaries //////////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI Entry=iri(Namespace, "entry");
	public static final IRI Key=iri(Namespace, "key");
	public static final IRI Value=iri(Namespace, "value");


	//// Audit Trail Records ///////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI Trace=iri(Namespace, "Trace"); // marker RDF class

	public static final IRI Item=iri(Namespace, "item"); // operation target resource (IRI)
	public static final IRI Task=iri(Namespace, "task"); // operation type tag (Value)
	public static final IRI User=iri(Namespace, "user"); // operation actor (IRI)
	public static final IRI Time=iri(Namespace, "time"); // operation ms-precision timestamp (xsd:dataTime)


	//// Utilities /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean interactive(final CharSequence header) {

		if ( header == null ) {
			throw new NullPointerException("null header");
		}

		return HTMLPattern.matcher(header).find();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Link() {} // a utility class

}
