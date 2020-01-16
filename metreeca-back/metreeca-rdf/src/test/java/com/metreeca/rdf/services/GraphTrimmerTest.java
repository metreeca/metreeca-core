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

package com.metreeca.rdf.services;

import com.metreeca.rdf.ValuesTest;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.ValuesTest.decode;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rdf.services.GraphTest.exec;
import static com.metreeca.tree.shapes.Field.field;

import static java.util.Collections.emptySet;


final class GraphTrimmerTest {

	private Collection<Statement> trim(final Shape shape, final Collection<Statement> model) {
		return new GraphTrimmer()
				.trim(new Response(new Request().base(ValuesTest.Base))
						.shape(shape)
						.body(rdf(), model)
				)
				.process(response -> response.body(rdf()))
				.fold(statements -> statements, failure -> emptySet());
	}


	@Test void testRetainCompatibleStatements() {
		exec(() -> assertThat(trim(field(RDF.FIRST, field(RDF.REST)), decode("<> rdf:first [rdf:rest rdf:nil].")))
				.hasSubset(decode("<> rdf:first [rdf:rest rdf:nil]."))
		);
	}

	@Test void testRemoveIncompatibleStatements() {
		exec(() -> assertThat(trim(field(RDF.FIRST), decode("<> rdf:rest rdf:nil.")))
				.isEmpty()
		);
	}

	@Test void testRemoveDisconnectedStatements() {
		exec(() -> assertThat(trim(field(RDF.FIRST), decode("rdf:nil rdf:value rdf:nil.")))
				.isEmpty()
		);
	}

}
