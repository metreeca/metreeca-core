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

import com.metreeca.json.Shape;
import com.metreeca.json.Values;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.json.Values.literal;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.Meta.alias;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Range.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


final class AndTest {

	@Nested final class Optimization {

		private final Value a=literal(1);
		private final Value b=literal(2);
		private final Value c=literal(3);


		@Test void testSimplifyConstants() {
			assertThat(and(or(), field(RDF.TYPE))).isEqualTo(or());
		}

		@Test void testUnwrapSingletons() {
			assertThat(and(datatype(RDF.NIL))).isEqualTo(datatype(RDF.NIL));
		}

		@Test void testUnwrapUniqueValues() {
			assertThat(and(datatype(RDF.NIL), datatype(RDF.NIL))).isEqualTo(datatype(RDF.NIL));
		}

		@Test void testCollapseDuplicates() {
			assertThat(and(datatype(RDF.NIL), datatype(RDF.NIL), minCount(1))).isEqualTo(and(datatype(RDF.NIL), minCount(1)));
		}

		@Test void testPreserveOrder() {
			assertThat(and(field(RDF.FIRST), field(RDF.REST)).map(new Shape.Probe<Collection<Shape>>() {

				@Override public Collection<Shape> probe(final And and) { return and.shapes(); }

			})).containsExactly(field(RDF.FIRST), field(RDF.REST));
		}


		@Test void testCollapseCompatibleAliases() {
			assertThat(and(alias("alias"), alias("alias")))
					.isEqualTo(alias("alias"));
		}

		@Test void testReportClashingAliases() {
			assertThatThrownBy(() -> and(alias("this"), alias("that")))
					.isInstanceOf(IllegalArgumentException.class);
		}


		@Test void testCollapseToAbstractDerivedDatatypes() {
			assertThat(and(datatype(Values.IRIType), datatype(Values.ResourceType)))
					.isEqualTo(datatype(Values.IRIType));
		}

		@Test void testCollapseToConcreteDerivedDatatypes() {
			assertThat(and(datatype(Values.LiteralType), datatype(XSD.STRING)))
					.isEqualTo(datatype(XSD.STRING));
		}

		@Test void testRetainUnrelatedDatatypes() {
			assertThat(and(datatype(Values.ResourceType), datatype(XSD.STRING)))
					.isEqualTo(and(datatype(Values.ResourceType), datatype(XSD.STRING)));
		}

		@Test void testOptimizeRange() {
			assertThat(and(range(a, b), range(b, c))).isEqualTo(range(b));
		}


		@Test void testOptimizeMinCount() {
			assertThat(and(minCount(10), minCount(100))).isEqualTo(minCount(100));
		}

		@Test void testOptimizeMaxCount() {
			assertThat(and(maxCount(10), maxCount(100))).isEqualTo(maxCount(10));
		}


		@Test void testOptimizeAll() {
			assertThat(and(all(a, b), all(b, c))).isEqualTo(all(a, b, c));
		}


		@Test void testMergeCompatibleFields() {
			assertThat(and(field(RDF.VALUE, minCount(1)), field(RDF.VALUE, maxCount(3))))
					.isEqualTo(field(RDF.VALUE, and(minCount(1), maxCount(3))));
		}

	}

}
