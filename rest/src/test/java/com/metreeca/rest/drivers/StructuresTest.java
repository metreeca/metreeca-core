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

package com.metreeca.rest.drivers;

import com.metreeca.form.things.ValuesTest;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Values.bnode;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.item;


final class StructuresTest {

	private static final IRI focus=item("focus");

	private static final BNode _dblank=bnode();
	private static final BNode _iblank=bnode();

	private static final IRI direct=item("direct");
	private static final IRI inverse=item("inverse");
	private static final IRI other=item("other");

	private static final Literal dlabel=literal(direct.getLocalName());
	private static final Literal dcomment=literal(direct.stringValue());
	private static final Literal ilabel=literal(inverse.getLocalName());
	private static final Literal icomment=literal(inverse.stringValue());

	//      +--------->_dblank+---->direct+------+
	//      |            +  ^         |          |
	//      +            |  |         v          v
	//    focus          |  |   label/comment  other
	//      ^            |  |         ^          +
	//      |            v  +         |          |
	//      +---------+_iblank<----+inverse<-----+

	private static final Collection<Statement> model=list(

			statement(focus, RDF.VALUE, _dblank),
			statement(_dblank, RDF.VALUE, direct),

			statement(inverse, RDF.VALUE, _iblank),
			statement(_iblank, RDF.VALUE, focus),

			statement(direct, RDF.VALUE, other),
			statement(other, RDF.VALUE, inverse),

			statement(_dblank, RDF.VALUE, _iblank),
			statement(_iblank, RDF.VALUE, _dblank),

			statement(direct, RDFS.LABEL, dlabel),
			statement(direct, RDFS.COMMENT, dcomment),

			statement(inverse, RDFS.LABEL, ilabel),
			statement(inverse, RDFS.COMMENT, icomment)

	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testRetrieveSymmetricConciseBoundedDescriptionFromModel() {

			final Model cell=Structures.description(focus, false, model);

			Assertions.assertThat(cell.subjects()).containsOnly(focus, _dblank, _iblank, inverse);
			Assertions.assertThat(cell.objects()).containsOnly(focus, _dblank, _iblank, direct);

	}

	@Test void testRetrieveLabelledSymmetricConciseBoundedDescriptionFromModel() {

		final Model cell=Structures.description(focus, true, model);

		Assertions.assertThat(cell.subjects())
				.containsOnly(focus, _dblank, _iblank, direct, inverse);

		Assertions.assertThat(cell.objects()).filteredOn(value -> value instanceof Resource)
				.containsOnly(focus, _dblank, _iblank, direct);

		Assertions.assertThat(cell.stream().filter(statement -> !statement.getPredicate().equals(RDF.VALUE))).containsOnly(

				statement(direct, RDFS.LABEL, dlabel),
				statement(direct, RDFS.COMMENT, dcomment),

				statement(inverse, RDFS.LABEL, ilabel),
				statement(inverse, RDFS.COMMENT, icomment)

		);

	}


	@Test void testRetrieveSymmetricConciseBoundedDescriptionFromRepository() {

		try (final RepositoryConnection connection=ValuesTest.sandbox(model).get()) {

			final Model cell=Structures.description(focus, false, connection);

			Assertions.assertThat(cell.subjects()).containsOnly(focus, _dblank, _iblank, inverse);
			Assertions.assertThat(cell.objects()).containsOnly(focus, _dblank, _iblank, direct);

		}

	}

	@Test void testRetrieveLabelledSymmetricConciseBoundedDescriptionFromRepository() {

		try (final RepositoryConnection connection=ValuesTest.sandbox(model).get()) {

			final Model cell=Structures.description(focus, true, connection);

			Assertions.assertThat(cell.subjects())
					.containsOnly(focus, _dblank, _iblank, direct, inverse);

			Assertions.assertThat(cell.objects()).filteredOn(value -> value instanceof Resource)
					.containsOnly(focus, _dblank, _iblank, direct);

			Assertions.assertThat(cell.stream().filter(statement -> !statement.getPredicate().equals(RDF.VALUE))).containsOnly(

					statement(direct, RDFS.LABEL, dlabel),
					statement(direct, RDFS.COMMENT, dcomment),

					statement(inverse, RDFS.LABEL, ilabel),
					statement(inverse, RDFS.COMMENT, icomment)

			);
		}

	}

}
