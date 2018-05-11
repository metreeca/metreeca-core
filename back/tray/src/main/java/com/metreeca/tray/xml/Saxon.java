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

package com.metreeca.tray.xml;

import com.metreeca.jeep.IO;
import com.metreeca.jeep.rdf.Values;
import com.metreeca.tray.Tool;

import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.s9api.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.io.StringReader;
import java.util.ServiceLoader;

import javax.xml.transform.stream.StreamSource;


/**
 * Saxon XML processor.
 */
public final class Saxon {

	public static final Tool<Saxon> Tool=tools -> new Saxon();


	private static final String identity=IO.text(Saxon.class, ".xsl");


	private final XQueryCompiler xquery; // thread-safe if not modified once initialized
	private final XsltCompiler xslt; // thread-safe if not modified once initialized


	public Saxon() {

		final Processor processor=new Processor(false);

		ServiceLoader.load(ExtensionFunctionDefinition.class).forEach(function ->
				processor.getUnderlyingConfiguration().registerExtensionFunction(function)
		);

		// !!! compiler.setErrorListener(???);

		xquery=processor.newXQueryCompiler();

		xquery.setLanguageVersion("3.1");

		xquery.declareNamespace("usr", Values.User);
		xquery.declareNamespace("html", "http://www.w3.org/1999/xhtml");
		xquery.declareNamespace("rdf", RDF.NAMESPACE);
		xquery.declareNamespace("rdfs", RDFS.NAMESPACE);
		xquery.declareNamespace("xsd", XMLSchema.NAMESPACE);

		xslt=processor.newXsltCompiler();

		xslt.setXsltLanguageVersion("2.0");
	}


	//// wrap compilers to prevent thread-unsafe modifications /////////////////////////////////////////////////////////

	public XQueryExecutable xquery(final String query) {
		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		try {

			return xquery.compile(query.isEmpty() ? "." : query);

		} catch ( final SaxonApiException e ) {
			throw new SaxonApiUncheckedException(e);
		}
	}

	public XsltExecutable xslt(final String transform) {

		if ( transform == null ) {
			throw new NullPointerException("null transform");
		}

		try {

			return xslt.compile(new StreamSource(new StringReader(transform.isEmpty() ? identity : transform)));

		} catch ( final SaxonApiException e ) {
			throw new SaxonApiUncheckedException(e);
		}
	}

}
