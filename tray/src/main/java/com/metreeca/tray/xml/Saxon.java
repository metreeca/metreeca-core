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

package com.metreeca.tray.xml;

import com.metreeca.form.things.Codecs;
import com.metreeca.form.things.Values;

import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.s9api.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.io.StringReader;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import javax.xml.transform.stream.StreamSource;


/**
 * Saxon XML processor.
 *
 * <p>Provides access to shared Saxon XQuery/XSLT processing tools.</p>
 */
public final class Saxon {

	/**
	 * The default XQuery language version.
	 */
	private static final String XQueryVersion="3.1";

	/**
	 * The default XSLT language version.
	 */
	private static final String XSLTVersion="2.0";

	/**
	 * Saxon XML processor factory.
	 *
	 * <p>The default processor acquired through this factory retrieves system resources from the classpath through
	 * {@link ClassLoader#getResourceAsStream(String)}.</p>
	 */
	public static final Supplier<Saxon> Factory=Saxon::new;


	private static final String identity=Codecs.text(Saxon.class, ".xsl");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final XQueryCompiler xquery; // thread-safe if not modified once initialized
	private final XsltCompiler xslt; // thread-safe if not modified once initialized


	/**
	 * Creates a Saxon XML processor.
	 */
	public Saxon() {

		final Processor processor=new Processor(false);

		ServiceLoader.load(ExtensionFunctionDefinition.class).forEach(function ->
				processor.getUnderlyingConfiguration().registerExtensionFunction(function)
		);

		// !!! compiler.setErrorListener(???);

		xquery=processor.newXQueryCompiler();

		xquery.setLanguageVersion(XQueryVersion);

		xquery.declareNamespace("app", Values.Internal);
		xquery.declareNamespace("html", "http://www.w3.org/1999/xhtml");
		xquery.declareNamespace("rdf", RDF.NAMESPACE);
		xquery.declareNamespace("rdfs", RDFS.NAMESPACE);
		xquery.declareNamespace("xsd", XMLSchema.NAMESPACE);

		xslt=processor.newXsltCompiler();

		xslt.setXsltLanguageVersion(XSLTVersion);
	}


	//// wrap compilers to prevent thread-unsafe modifications /////////////////////////////////////////////////////////

	/**
	 * Compiles XQuery queries.
	 *
	 * <p>Presets XQuery language version at {@value XQueryVersion} and defines prefixes for the following default
	 * namespaces:</p>
	 *
	 * <ul>
	 * <li>{@code app} = {@value Values#Internal}</li>
	 * <li>{@code html} = "http://www.w3.org/1999/xhtml"</li>
	 * <li>{@code rdf} = {@value RDF#NAMESPACE}</li>
	 * <li>{@code rdfs} = {@value RDFS#NAMESPACE}</li>
	 * <li>{@code xsd} = {@value XMLSchema#NAMESPACE}</li>
	 * </ul>
	 *
	 * @param query the XQuery query to be compiled
	 *
	 * @return the compiled {@code xquery} query; empty for the identity query
	 *
	 * @throws NullPointerException       if {@code query} is null
	 * @throws SaxonApiUncheckedException if a compilation error occurs
	 */
	public XQueryExecutable xquery(final String query) throws SaxonApiUncheckedException {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		try {

			return xquery.compile(query.isEmpty() ? "." : query);

		} catch ( final SaxonApiException e ) {
			throw new SaxonApiUncheckedException(e);
		}
	}

	/**
	 * Compiles XSLT transforms.
	 *
	 * <p>Presets XSLT language version at {@value XSLTVersion}.
	 *
	 * @param transform the XSLT transform to be compiled; empty for the identity transform
	 *
	 * @return the compiled {@code transform}
	 *
	 * @throws NullPointerException       if {@code transform} is null
	 * @throws SaxonApiUncheckedException if a compilation error occurs
	 */
	public XsltExecutable xslt(final String transform) throws SaxonApiUncheckedException {

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
