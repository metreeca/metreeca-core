/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.form.things;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Structures.description;
import static com.metreeca.form.things.Values.bnode;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.item;

import static org.assertj.core.api.Assertions.assertThat;


final class StructuresTest {

	@Nested final class Descriptions {

		private final IRI focus=item("focus");

		private final BNode _dblank=bnode();
		private final BNode _iblank=bnode();

		private final IRI direct=item("direct");
		private final IRI inverse=item("inverse");
		private final IRI other=item("other");

		private final Literal dlabel=literal(direct.getLocalName());
		private final Literal dcomment=literal(direct.stringValue());
		private final Literal ilabel=literal(inverse.getLocalName());
		private final Literal icomment=literal(inverse.stringValue());

		//      +--------->_dblank+---->direct+------+
		//      |            +  ^         |          |
		//      +            |  |         v          v
		//    focus          |  |   label/comment  other
		//      ^            |  |         ^          +
		//      |            v  +         |          |
		//      +---------+_iblank<----+inverse<-----+

		private final Collection<Statement> model=list(

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


		@Test void testRetrieveSymmetricConciseBoundedDescription() {

			final Model cell=new LinkedHashModel(description(focus, false, model));

			assertThat(cell.subjects()).containsOnly(focus, _dblank, _iblank, inverse);
			assertThat(cell.objects()).containsOnly(focus, _dblank, _iblank, direct);

		}

		@Test void testRetrieveLabelledSymmetricConciseBoundedDescription() {

			final Model cell=(Model)description(focus, true, model);

			assertThat(cell.subjects())
					.containsOnly(focus, _dblank, _iblank, direct, inverse);

			assertThat(cell.objects()).filteredOn(value -> value instanceof Resource)
					.containsOnly(focus, _dblank, _iblank, direct);

			assertThat(cell.stream().filter(statement -> !statement.getPredicate().equals(RDF.VALUE))).containsOnly(

					statement(direct, RDFS.LABEL, dlabel),
					statement(direct, RDFS.COMMENT, dcomment),

					statement(inverse, RDFS.LABEL, ilabel),
					statement(inverse, RDFS.COMMENT, icomment)

			);

		}

	}

}
