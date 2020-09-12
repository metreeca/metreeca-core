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

package com.metreeca.json.shapes;

import com.metreeca.json.Values;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.Meta.alias;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


final class OrTest {

	@Nested final class Optimization {

		@Test void testSimplifyConstants() {
			assertThat(or(and(), field(RDF.TYPE))).isEqualTo(and());
		}

		@Test void testUnwrapSingletons() {
			assertThat(or(datatype(RDF.NIL))).isEqualTo(datatype(RDF.NIL));
		}

		@Test void testUnwrapUniqueValues() {
			assertThat(or(datatype(RDF.NIL), datatype(RDF.NIL))).isEqualTo(datatype(RDF.NIL));
		}

		@Test void testCollapseDuplicates() {
			assertThat(or(datatype(RDF.NIL), datatype(RDF.NIL), minCount(1))).isEqualTo(or(datatype(RDF.NIL), minCount(1)));
		}


		@Test void testCollapseCompatibleAliases() {
			assertThat(or(alias("alias"), alias("alias")))
					.isEqualTo(alias("alias"));
		}

		@Test void testReportClashingAliases() {
			assertThatThrownBy(() -> or(alias("this"), alias("that")))
					.isInstanceOf(IllegalArgumentException.class);
		}


		@Test void testCollapseFromAbstractDeriveDatatypes() {
			assertThat(or(datatype(Values.IRIType), datatype(Values.ResourceType)))
					.isEqualTo(datatype(Values.ResourceType));
		}

		@Test void testCollapseFromConcreteDerivedDatatypes() {
			assertThat(or(datatype(Values.LiteralType), datatype(XSD.STRING)))
					.isEqualTo(datatype(Values.LiteralType));
		}

		@Test void testRetainUnrelatedDatatypes() {
			assertThat(or(datatype(Values.ResourceType), datatype(XSD.STRING)))
					.isEqualTo(or(datatype(Values.ResourceType), datatype(XSD.STRING)));
		}


		@Test void testOptimizeMinCount() {
			assertThat(or(minCount(10), minCount(100))).isEqualTo(minCount(10));
		}

		@Test void testOptimizeMaxCount() {
			assertThat(or(maxCount(10), maxCount(100))).isEqualTo(maxCount(100));
		}


		@Test void testMergeCompatibleFields() {
			assertThat(or(field(RDF.VALUE, minCount(1)), field(RDF.VALUE, maxCount(3))))
					.isEqualTo(field(RDF.VALUE, or(minCount(1), maxCount(3))));
		}

	}

}
