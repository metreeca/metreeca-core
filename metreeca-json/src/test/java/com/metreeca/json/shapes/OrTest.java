/*
 * Copyright © 2013-2020 Metreeca srl
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
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Lang.lang;
import static com.metreeca.json.shapes.Localized.localized;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.Meta.alias;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Range.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


final class OrTest {

	@Nested final class Optimization {

		private final Value a=literal(1);
		private final Value b=literal(2);
		private final Value c=literal(3);


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
			assertThat(or(datatype(RDF.NIL), datatype(RDF.NIL), minCount(1))).isEqualTo(or(datatype(RDF.NIL),
					minCount(1)));
		}

		@Test void testPreserveOrder() {
			assertThat(or(field(RDF.FIRST), field(RDF.REST)).map(new Shape.Probe<Collection<Shape>>() {

				@Override public Collection<Shape> probe(final Or or) { return or.shapes(); }

			})).containsExactly(field(RDF.FIRST), field(RDF.REST));
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

		@Test void testOptimizeRange() {
			assertThat(or(range(a, b), range(b, c))).isEqualTo(range(a, b, c));
		}

		@Test void testOptimizeLang() {
			assertThat(or(lang("en", "it"), lang("en", "fr"))).isEqualTo(lang("en", "it", "fr"));
		}


		@Test void testOptimizeMinCount() {
			assertThat(or(minCount(10), minCount(100))).isEqualTo(minCount(10));
		}

		@Test void testOptimizeMaxCount() {
			assertThat(or(maxCount(10), maxCount(100))).isEqualTo(maxCount(100));
		}

		@Test void testOptimizeAny() {
			assertThat(or(any(a, b), any(b, c))).isEqualTo(any(a, b, c));
		}

		@Test void testOptimizeLocalized() {
			assertThat(or(localized(), localized())).isEqualTo(localized());
		}


		@Test void testMergeCompatibleFields() {
			assertThat(or(field(RDF.VALUE, minCount(1)), field(RDF.VALUE, maxCount(3))))
					.isEqualTo(field(RDF.VALUE, or(minCount(1), maxCount(3))));
		}

	}

}
