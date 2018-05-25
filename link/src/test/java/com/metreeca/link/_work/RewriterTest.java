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

package com.metreeca.link._work;

import com.metreeca.spec.things.ValuesTest;
import com.metreeca.spec.things.Transputs;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.junit.Test;

import java.io.*;

import static com.metreeca.link._work.Rewriter.rewriter;
import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.spec.things.Values.statement;

import static org.junit.Assert.assertEquals;


public final class RewriterTest {

	public static final String External=ValuesTest.Base;
	public static final String Internal="app://test/";


	public static IRI external(final String name) {
		return iri(External, name);
	}

	public static IRI internal(final String name) {
		return iri(Internal, name);
	}


	public static Statement internal(final String subject, final String predicate, final String object) {
		return statement(internal(subject), internal(predicate), internal(object));
	}

	public static Statement external(final String subject, final String predicate, final String object) {
		return statement(external(subject), external(predicate), external(object));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testRewriteReader() throws IOException {

		final Reader external=new StringReader("<"+external("test")+">");
		final Reader internal=rewriter(External, Internal).internal(external);

		assertEquals("reader rewritten", "<"+internal("test")+">", Transputs.text(internal));
	}

	@Test public void testRewriteWriter() throws IOException {

		final StringWriter external=new StringWriter();

		try (final Writer internal=rewriter(External, Internal).external(external)) {
			internal.write("<"+internal("test")+">");
		}

		assertEquals("writer rewritten", "<"+external("test")+">", external.toString());
	}


	@Test(expected=IllegalArgumentException.class) public void testRejectRelativeBase() {
		rewriter("/examle.org/", "");
	}

	@Test(expected=IllegalArgumentException.class) public void testRejectMalformedBase() {
		rewriter("http://examle.org", "");
	}

}
